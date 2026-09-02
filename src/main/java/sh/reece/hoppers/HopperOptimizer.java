package sh.reece.hoppers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import sh.reece.tools.Main;
import sh.reece.tools.Unloadable;
import sh.reece.utiltools.Util;

/**
 * EXPERIMENTAL toggleable hopper optimizer. Off by default.
 *
 * Vanilla ticks every loaded hopper each tick; the costly bits are the neighbour scan
 * and an AABB entity search for dropped items. On a spread-out skyblock with thousands
 * of hoppers that dominates the tick.
 *
 * This freezes vanilla hopper ticking (cooldown-pinning via {@link HopperNMS}) and does
 * the work itself, cheaper:
 *   1. event-driven pickup - dropped items are routed straight into the hopper below on
 *      {@link ItemSpawnEvent}, so we never run per-hopper AABB scans (the big win).
 *   2. round-robin transfer budget - only {@code BudgetPerTick} hoppers are serviced per
 *      cycle, smoothing MSPT instead of spiking.
 *   3. per-hopper verified takeover - a hopper is only managed if its cooldown pin is
 *      read-back confirmed, otherwise vanilla keeps it (no double transfers / dupes).
 *
 * Not yet handled (falls back to plain moves / may differ from vanilla): furnaces,
 * brewing stands, hopper minecarts, tick-perfect redstone item sorters.
 */
public class HopperOptimizer implements Listener, Unloadable {

	private static final String SECTION = "Hoppers.Optimize";
	static HopperOptimizer INSTANCE;

	private final Main plugin;
	private final HopperNMS nms = new HopperNMS();

	private boolean active;
	private int budgetPerTick;
	private int interval;
	private int amount;
	private int pinValue;
	private final Set<String> disabledWorlds = new HashSet<>();

	// managed = hoppers whose cooldown pin was confirmed; only these are ours to move.
	private final LinkedHashSet<Block> managed = new LinkedHashSet<>();
	private List<Block> ring = new ArrayList<>();
	private boolean ringDirty;
	private int cursor;

	private BukkitTask task;

	// diagnostics for /hopperopt
	private long transfers;
	private long pickups;

	public HopperOptimizer(Main instance) {
		INSTANCE = this;
		this.plugin = instance;

		if (!instance.getConfigUtils().enabledInConfig(SECTION + ".Enabled")) {
			return;
		}

		this.budgetPerTick = Math.max(1, instance.getConfig().getInt(SECTION + ".BudgetPerTick", 512));
		this.interval = Math.max(1, instance.getConfig().getInt(SECTION + ".TransferInterval", 8));
		this.amount = Math.max(1, instance.getConfig().getInt(SECTION + ".TransferAmount", 1));
		this.pinValue = Math.max(4, interval * 3);
		for (String w : instance.getConfig().getStringList(SECTION + ".DisabledWorlds")) {
			disabledWorlds.add(w.toLowerCase());
		}

		if (!nms.bind()) {
			Util.consoleMSG("&c[Hoppers] optimizer disabled - could not bind to server internals.");
			return;
		}

		active = true;
		Bukkit.getPluginManager().registerEvents(this, instance);
		task = Bukkit.getScheduler().runTaskTimer(instance, this::tick, interval, interval);
		scanLoadedChunks();
		Util.consoleMSG("&a[Hoppers] optimizer ON &7(managing " + managed.size()
			+ " hoppers, budget " + budgetPerTick + "/" + interval + "t)");
	}

	// registration / lifecycle

	private void scanLoadedChunks() {
		for (World world : Bukkit.getWorlds()) {
			if (skip(world)) {
				continue;
			}
			for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
				for (BlockState state : chunk.getTileEntities()) {
					if (state instanceof Hopper) {
						manage(state.getBlock());
					}
				}
			}
		}
	}

	private boolean skip(World world) {
		return disabledWorlds.contains(world.getName().toLowerCase());
	}

	private void manage(Block block) {
		if (managed.contains(block)) {
			return;
		}
		if (nms.pin(block, pinValue)) {
			managed.add(block);
			ringDirty = true;
		}
		// pin unconfirmed -> leave vanilla in charge of this hopper.
	}

	private void unmanage(Block block) {
		if (managed.remove(block)) {
			nms.reset(block); // hand back to vanilla, clean NBT
			ringDirty = true;
		}
	}

	// events

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onChunkLoad(ChunkLoadEvent e) {
		if (skip(e.getWorld())) {
			return;
		}
		for (BlockState state : e.getChunk().getTileEntities()) {
			if (state instanceof Hopper) {
				manage(state.getBlock());
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onChunkUnload(ChunkUnloadEvent e) {
		for (BlockState state : e.getChunk().getTileEntities()) {
			if (state instanceof Hopper) {
				unmanage(state.getBlock());
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlace(BlockPlaceEvent e) {
		Block b = e.getBlock();
		if (b.getType() == Material.HOPPER && !skip(b.getWorld())) {
			manage(b);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBreak(BlockBreakEvent e) {
		if (e.getBlock().getType() == Material.HOPPER) {
			unmanage(e.getBlock());
		}
	}

	/** Push dropped items straight into the hopper below - replaces vanilla's AABB suck. */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onItemSpawn(ItemSpawnEvent e) {
		if (managed.isEmpty()) {
			return;
		}
		Item item = e.getEntity();
		Block at = item.getLocation().getBlock();
		Block hopperBlock = hopperOrNull(at);
		if (hopperBlock == null) {
			hopperBlock = hopperOrNull(at.getRelative(BlockFace.DOWN));
		}
		if (hopperBlock == null || hopperBlock.isBlockIndirectlyPowered()) {
			return;
		}
		BlockState st = hopperBlock.getState(false);
		if (!(st instanceof Hopper)) {
			return;
		}
		ItemStack stack = item.getItemStack();
		HashMap<Integer, ItemStack> left = ((Hopper) st).getInventory().addItem(stack);
		if (left.isEmpty()) {
			item.remove();
			pickups++;
		} else {
			ItemStack remainder = left.values().iterator().next();
			if (remainder.getAmount() != stack.getAmount()) {
				item.setItemStack(remainder);
				pickups++;
			}
		}
	}

	private Block hopperOrNull(Block b) {
		return (b.getType() == Material.HOPPER && managed.contains(b)) ? b : null;
	}

	// transfer engine

	private void tick() {
		if (ringDirty) {
			ring = new ArrayList<>(managed);
			ringDirty = false;
			if (cursor >= ring.size()) {
				cursor = 0;
			}
		}
		int size = ring.size();
		if (size == 0) {
			return;
		}
		int n = Math.min(budgetPerTick, size);
		for (int i = 0; i < n; i++) {
			if (cursor >= size) {
				cursor = 0;
			}
			process(ring.get(cursor++));
		}
	}

	private void process(Block block) {
		if (block.getType() != Material.HOPPER) {
			unmanage(block); // stale entry
			return;
		}
		// keep vanilla frozen; if the pin can't be reconfirmed, hand back to vanilla.
		if (!nms.pin(block, pinValue)) {
			unmanage(block);
			return;
		}
		if (block.isBlockIndirectlyPowered()) {
			return; // locked hopper
		}
		BlockState st = block.getState(false);
		if (!(st instanceof Hopper)) {
			return;
		}
		Inventory self = ((Hopper) st).getInventory();

		// eject into the container the hopper faces
		BlockFace face = ((org.bukkit.block.data.type.Hopper) block.getBlockData()).getFacing();
		Inventory out = containerInv(block.getRelative(face));
		if (out != null && moveOne(self, out)) {
			transfers++;
		}

		// suck from the container directly above
		Inventory in = containerInv(block.getRelative(BlockFace.UP));
		if (in != null && moveOne(in, self)) {
			transfers++;
		}
	}

	private Inventory containerInv(Block block) {
		if (!block.getType().isBlock() || block.isEmpty()) {
			return null;
		}
		BlockState st = block.getState(false);
		return (st instanceof Container) ? ((Container) st).getInventory() : null;
	}

	/** Move up to {@code amount} of the first non-empty slot from -> to. Returns true if moved. */
	private boolean moveOne(Inventory from, Inventory to) {
		for (int i = 0; i < from.getSize(); i++) {
			ItemStack it = from.getItem(i);
			if (it == null || it.getType().isAir()) {
				continue;
			}
			ItemStack move = it.clone();
			move.setAmount(Math.min(amount, it.getAmount()));
			HashMap<Integer, ItemStack> left = to.addItem(move);
			int rejected = left.isEmpty() ? 0 : left.values().iterator().next().getAmount();
			int moved = move.getAmount() - rejected;
			if (moved > 0) {
				it.setAmount(it.getAmount() - moved);
				from.setItem(i, it.getAmount() <= 0 ? null : it);
				return true;
			}
		}
		return false;
	}

	@Override
	public void onUnload() {
		if (task != null) {
			task.cancel();
		}
		for (Block block : new ArrayList<>(managed)) {
			nms.reset(block); // un-freeze so vanilla resumes
		}
		managed.clear();
		ring.clear();
		active = false;
	}

	// diagnostics

	public static String statusText() {
		if (INSTANCE == null || !INSTANCE.active) {
			return Util.color("&e[Hoppers] optimizer is &cOFF&e (disabled in config or NMS bind failed).");
		}
		HopperOptimizer o = INSTANCE;
		return Util.color(
			"&a[Hoppers] optimizer ON\n"
			+ "&7 managed hoppers: &f" + o.managed.size() + "\n"
			+ "&7 budget: &f" + o.budgetPerTick + "&7/&f" + o.interval + "t&7, amount &f" + o.amount + "\n"
			+ "&7 transfers: &f" + o.transfers + " &7pickups: &f" + o.pickups);
	}
}
