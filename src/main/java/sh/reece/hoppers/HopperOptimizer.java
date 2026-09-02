package sh.reece.hoppers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitTask;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import sh.reece.tools.Main;
import sh.reece.tools.Unloadable;
import sh.reece.utiltools.Util;

/**
 * EXPERIMENTAL toggleable hopper optimizer. Off by default.
 *
 * Vanilla ticks every loaded hopper each tick; the costly bits are the neighbour scan and
 * an AABB entity search for dropped items. On a spread-out skyblock with tens of thousands
 * of hoppers that dominates the tick, even though almost all of them are idle.
 *
 * Rather than re-implement transfers (which breaks sorters, comparators and protection
 * plugins), this just puts idle hoppers to sleep and lets vanilla do the real work:
 *
 *   - HOT (awake): a small set of hoppers with recent activity. Their cooldown is left at 0
 *     so vanilla ticks them at full speed - normal transfers, item pickup, sorting, filters.
 *   - COLD (sleeping): everything else. Their cooldown is pinned via {@link HopperNMS} so
 *     vanilla skips them entirely. We only top the pin back up on a slow round-robin, so a
 *     cold hopper costs ~one field write every few seconds instead of a full vanilla tick.
 *
 * A hopper goes hot when there's work to do (an item drops on/rests above it, a neighbouring
 * container changes, redstone toggles, it's placed) and decays back to cold after
 * {@code AwakeTicks} of quiet. Because vanilla stays the transfer engine, behaviour is
 * identical to stock - the only change is that idle hoppers stop burning tick time.
 *
 * Fail-safe: {@code SleepTicks} is modest, so if the plugin ever stops, vanilla drains the
 * pinned cooldown and every hopper resumes normal behaviour within that window - no hopper
 * is left frozen on disk.
 */
public class HopperOptimizer implements Listener, Unloadable {

	private static final String SECTION = "Hoppers.Optimize";
	private static final BlockFace[] NEIGHBOURS = {
		BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
	};
	static HopperOptimizer INSTANCE;

	private final Main plugin;
	private final HopperNMS nms = new HopperNMS();

	private boolean active;
	private int awakeTicks;       // ticks a hopper stays hot after its last activity
	private int sleepTicks;       // cooldown we pin cold hoppers to (also the self-heal window)
	private int itemSweepInterval; // ticks between item-entity sweeps
	private int transferAmount;   // items vanilla moves per transfer (Spigot hopper-amount)
	private int repinPeriod;      // spread cold re-pins over this many ticks
	private final Set<String> disabledWorlds = new HashSet<>();

	private final java.util.Map<UUID, WorldHoppers> worlds = new java.util.HashMap<>();
	private long now;

	private BukkitTask task;

	// diagnostics for /hopperopt
	private long wakes;
	private long sweeps;

	public HopperOptimizer(Main instance) {
		INSTANCE = this;
		this.plugin = instance;

		if (!instance.getConfigUtils().enabledInConfig(SECTION + ".Enabled")) {
			return;
		}

		// This engine runs one global per-tick timer that scans hoppers across every world
		// and pins NMS cooldowns - there is no such global thread under Folia's region
		// threading, so the feature can't exist there. Gate it off (stays fully Paper-only).
		if (sh.reece.utiltools.Schedulers.isFolia()) {
			Util.consoleMSG("&e[Hoppers] optimizer disabled - not supported on Folia (Paper only).");
			return;
		}

		this.awakeTicks = Math.max(1, instance.getConfig().getInt(SECTION + ".AwakeTicks", 60));
		this.sleepTicks = Math.max(20, instance.getConfig().getInt(SECTION + ".SleepTicks", 100));
		this.itemSweepInterval = Math.max(1, instance.getConfig().getInt(SECTION + ".ItemSweepInterval", 10));
		this.transferAmount = Math.max(1, instance.getConfig().getInt(SECTION + ".TransferAmount", 1));
		this.repinPeriod = Math.max(1, sleepTicks - 20); // re-pin before the pin can drain to 0
		for (String w : instance.getConfig().getStringList(SECTION + ".DisabledWorlds")) {
			disabledWorlds.add(w.toLowerCase());
		}

		if (!nms.bind()) {
			Util.consoleMSG("&c[Hoppers] optimizer disabled - could not bind to server internals.");
			return;
		}

		active = true;
		Bukkit.getPluginManager().registerEvents(this, instance);
		task = Bukkit.getScheduler().runTaskTimer(instance, this::tick, 1L, 1L);
		scanLoadedChunks();
		Util.consoleMSG("&a[Hoppers] optimizer ON &7(managing " + managedCount() + " hoppers)");
	}

	// per-world state. hot = awake (packed pos -> tick it should sleep at); cold = sleeping.

	private static final class WorldHoppers {
		final World world;
		final LongOpenHashSet cold = new LongOpenHashSet();
		final Long2LongOpenHashMap hot = new Long2LongOpenHashMap();
		long[] coldRing = new long[0];
		int cursor;
		boolean ringDirty;

		WorldHoppers(World world) {
			this.world = world;
			hot.defaultReturnValue(Long.MIN_VALUE);
		}

		boolean has(long packed) {
			return cold.contains(packed) || hot.containsKey(packed);
		}
	}

	// bit-packed block position (26-bit signed x/z, 12-bit signed y) - matches vanilla BlockPos.

	private static long pack(int x, int y, int z) {
		return ((x & 0x3FFFFFFL) << 38) | ((z & 0x3FFFFFFL) << 12) | (y & 0xFFFL);
	}

	private static int unpackX(long p) {
		return (int) (p >> 38);
	}

	private static int unpackY(long p) {
		return (int) (p << 52 >> 52);
	}

	private static int unpackZ(long p) {
		return (int) (p << 26 >> 38);
	}

	private WorldHoppers worldData(World world, boolean create) {
		WorldHoppers wh = worlds.get(world.getUID());
		if (wh == null && create) {
			wh = new WorldHoppers(world);
			worlds.put(world.getUID(), wh);
			if (!nms.setHopperAmount(world, transferAmount)) {
				Util.log("&e[Hoppers] couldn't set hopper-amount for " + world.getName() + " - using server default.");
			}
		}
		return wh;
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

	/**
	 * Start managing a hopper. New hoppers begin HOT so vanilla drains any pending work before
	 * they settle to cold - a sorter loaded mid-transfer never gets frozen with items in it.
	 */
	private void manage(Block block) {
		WorldHoppers wh = worldData(block.getWorld(), true);
		long packed = pack(block.getX(), block.getY(), block.getZ());
		if (wh.has(packed)) {
			return;
		}
		nms.reset(block); // ensure it's ticking; it'll be pinned once it goes idle
		wh.hot.put(packed, now + awakeTicks);
	}

	private void unmanage(Block block) {
		WorldHoppers wh = worldData(block.getWorld(), false);
		if (wh == null) {
			return;
		}
		long packed = pack(block.getX(), block.getY(), block.getZ());
		if (wh.cold.remove(packed)) {
			wh.ringDirty = true;
		}
		wh.hot.remove(packed);
		nms.reset(block); // hand back to vanilla, clean NBT
	}

	// wake helpers - promote a cold hopper to hot so vanilla services it next tick.

	private void wake(World world, long packed) {
		WorldHoppers wh = worldData(world, false);
		if (wh == null || !wh.has(packed)) {
			return;
		}
		if (wh.cold.remove(packed)) {
			wh.ringDirty = true;
			nms.resetAt(world, unpackX(packed), unpackY(packed), unpackZ(packed));
			wakes++;
		}
		wh.hot.put(packed, now + awakeTicks); // (re)arm the idle timer
	}

	private void wake(Block block) {
		wake(block.getWorld(), pack(block.getX(), block.getY(), block.getZ()));
	}

	/** Wake a managed hopper at this block and any managed hopper touching it. */
	private void wakeAround(Block block) {
		wake(block);
		for (BlockFace face : NEIGHBOURS) {
			Block n = block.getRelative(face);
			if (n.getType() == Material.HOPPER) {
				wake(n);
			}
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
		WorldHoppers wh = worldData(e.getWorld(), false);
		if (wh == null) {
			return;
		}
		for (BlockState state : e.getChunk().getTileEntities()) {
			if (state instanceof Hopper) {
				unmanage(state.getBlock());
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlace(BlockPlaceEvent e) {
		Block b = e.getBlock();
		if (skip(b.getWorld())) {
			return;
		}
		if (b.getType() == Material.HOPPER) {
			manage(b);
		} else {
			wakeAround(b); // a container placed next to a sleeping hopper is new work
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBreak(BlockBreakEvent e) {
		Block b = e.getBlock();
		if (b.getType() == Material.HOPPER) {
			unmanage(b);
		} else {
			wakeAround(b);
		}
	}

	/** Redstone toggling a hopper's lock changes what it should do - wake it and its neighbours. */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onRedstone(BlockRedstoneEvent e) {
		wakeAround(e.getBlock());
	}

	/** Item dropped on/above a managed hopper - wake it so vanilla's next tick sucks it in. */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onItemSpawn(ItemSpawnEvent e) {
		wakeUnderItem(e.getEntity());
	}

	/** A hopper just pulled an item entity - keep it hot while items keep coming. */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPickup(InventoryPickupItemEvent e) {
		Block b = holderBlock(e.getInventory().getHolder());
		if (b != null) {
			wake(b);
		}
	}

	/** Vanilla moved items between containers - keep both ends (and their neighbours) hot so chains flow. */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onMove(InventoryMoveItemEvent e) {
		Block src = holderBlock(e.getSource().getHolder());
		Block dst = holderBlock(e.getDestination().getHolder());
		if (src != null) {
			wakeAround(src);
		}
		if (dst != null) {
			wakeAround(dst);
		}
	}

	/** A player edited a container - a hopper feeding from or into it may now have work. */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onContainerClose(InventoryCloseEvent e) {
		Block b = holderBlock(e.getInventory().getHolder());
		if (b != null) {
			wakeAround(b);
		}
	}

	private Block holderBlock(InventoryHolder holder) {
		if (holder instanceof BlockState) {
			return ((BlockState) holder).getBlock();
		}
		if (holder instanceof org.bukkit.block.DoubleChest) {
			InventoryHolder left = ((org.bukkit.block.DoubleChest) holder).getLeftSide();
			if (left instanceof BlockState) {
				return ((BlockState) left).getBlock();
			}
		}
		return null;
	}

	private void wakeUnderItem(Item item) {
		Block at = item.getLocation().getBlock();
		if (at.getType() == Material.HOPPER) {
			wake(at);
		}
		Block below = at.getRelative(BlockFace.DOWN);
		if (below.getType() == Material.HOPPER) {
			wake(below);
		}
	}

	// engine - runs every tick, but only touches the small hot set plus a thin slice of cold.

	private void tick() {
		now++;
		for (WorldHoppers wh : worlds.values()) {
			expireHot(wh);
			repinColdSlice(wh);
		}
		if (now % itemSweepInterval == 0) {
			sweepItems();
		}
	}

	/** Hot hoppers past their idle deadline go cold: pin them once and drop them from the hot set. */
	private void expireHot(WorldHoppers wh) {
		if (wh.hot.isEmpty()) {
			return;
		}
		var it = wh.hot.long2LongEntrySet().fastIterator();
		while (it.hasNext()) {
			Long2LongMap.Entry entry = it.next();
			if (now < entry.getLongValue()) {
				continue;
			}
			long packed = entry.getLongKey();
			int x = unpackX(packed), y = unpackY(packed), z = unpackZ(packed);
			it.remove();
			if (wh.world.getBlockAt(x, y, z).getType() != Material.HOPPER) {
				nms.resetAt(wh.world, x, y, z);
				continue; // block changed under us - forget it
			}
			if (nms.pinAt(wh.world, x, y, z, sleepTicks)) {
				wh.cold.add(packed);
				wh.ringDirty = true;
			}
			// pin unconfirmed -> leave it to vanilla (dropped from both sets)
		}
	}

	/**
	 * Top up the cooldown pin on a slice of cold hoppers so the whole cold set is refreshed
	 * every {@code repinPeriod} ticks. This is the only per-tick cost that scales with hopper
	 * count, and it's just a verified field write - no inventory or entity scans.
	 */
	private void repinColdSlice(WorldHoppers wh) {
		int size = wh.cold.size();
		if (size == 0) {
			return;
		}
		if (wh.ringDirty || wh.coldRing.length != size) {
			rebuildRing(wh);
		}
		int budget = Math.max(1, (size + repinPeriod - 1) / repinPeriod); // ceil(size / period)
		for (int i = 0; i < budget; i++) {
			if (wh.cursor >= wh.coldRing.length) {
				wh.cursor = 0;
			}
			long packed = wh.coldRing[wh.cursor++];
			if (!wh.cold.contains(packed)) {
				continue; // woken since the ring was built
			}
			int x = unpackX(packed), y = unpackY(packed), z = unpackZ(packed);
			if (wh.world.getBlockAt(x, y, z).getType() != Material.HOPPER
				|| !nms.pinAt(wh.world, x, y, z, sleepTicks)) {
				wh.cold.remove(packed);
				wh.ringDirty = true;
			}
		}
	}

	private void rebuildRing(WorldHoppers wh) {
		wh.coldRing = new long[wh.cold.size()];
		int i = 0;
		for (LongIterator it = wh.cold.iterator(); it.hasNext(); ) {
			wh.coldRing[i++] = it.nextLong();
		}
		wh.ringDirty = false;
		if (wh.cursor >= wh.coldRing.length) {
			wh.cursor = 0;
		}
	}

	/**
	 * Wake hoppers sitting under dropped items. Iterates item entities (few) not hoppers (many),
	 * so it catches items that landed/flowed onto a hopper after spawning - the case pure
	 * {@link ItemSpawnEvent} misses.
	 */
	private void sweepItems() {
		for (WorldHoppers wh : worlds.values()) {
			if (wh.cold.isEmpty() && wh.hot.isEmpty()) {
				continue;
			}
			for (Item item : wh.world.getEntitiesByClass(Item.class)) {
				wakeUnderItem(item);
			}
		}
		sweeps++;
	}

	@Override
	public void onUnload() {
		if (task != null) {
			task.cancel();
		}
		for (WorldHoppers wh : worlds.values()) {
			for (LongIterator it = wh.cold.iterator(); it.hasNext(); ) {
				long p = it.nextLong();
				nms.resetAt(wh.world, unpackX(p), unpackY(p), unpackZ(p)); // un-freeze so vanilla resumes
			}
			for (long p : wh.hot.keySet()) {
				nms.resetAt(wh.world, unpackX(p), unpackY(p), unpackZ(p));
			}
		}
		worlds.clear();
		active = false;
	}

	// diagnostics

	private int managedCount() {
		int n = 0;
		for (WorldHoppers wh : worlds.values()) {
			n += wh.cold.size() + wh.hot.size();
		}
		return n;
	}

	public static String statusText() {
		if (INSTANCE == null || !INSTANCE.active) {
			return Util.color("&e[Hoppers] optimizer is &cOFF&e (disabled in config or NMS bind failed).");
		}
		HopperOptimizer o = INSTANCE;
		int hot = 0, cold = 0;
		for (WorldHoppers wh : o.worlds.values()) {
			hot += wh.hot.size();
			cold += wh.cold.size();
		}
		return Util.color(
			"&a[Hoppers] optimizer ON\n"
			+ "&7 managed: &f" + (hot + cold) + " &7(&fhot " + hot + "&7, &fcold " + cold + "&7)\n"
			+ "&7 awake &f" + o.awakeTicks + "t&7, sleep &f" + o.sleepTicks + "t&7, sweep &f" + o.itemSweepInterval + "t&7, amount &f" + o.transferAmount + "\n"
			+ "&7 wakes: &f" + o.wakes + " &7sweeps: &f" + o.sweeps);
	}
}
