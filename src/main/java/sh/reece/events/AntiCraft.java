package sh.reece.events;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class AntiCraft extends BaseCommand implements Listener {

	private static java.io.File f;
	private static org.bukkit.configuration.file.FileConfiguration storage;
	private String Message, bypass, AdminPerm;
	private ConfigUtils configUtils;

	// cached: material -> durabilities for O(1) lookups
	private static Map<Material, Set<Short>> blockedCache = new HashMap<>();

	// per-station toggles + Material-only block lists (crafting table keeps the rich durability model above)
	private boolean craftingEnabled, anvilEnabled, smithingEnabled, grindstoneEnabled, enchantingEnabled;
	private Set<Material> bannedAnvil = EnumSet.noneOf(Material.class);
	private Set<Material> bannedSmithing = EnumSet.noneOf(Material.class);
	private Set<Material> bannedGrindstone = EnumSet.noneOf(Material.class);
	private Set<Material> bannedEnchanting = EnumSet.noneOf(Material.class);

	public AntiCraft(Main instance) {
		super(instance, "Events.AntiCraft", "AntiCraft");
		configUtils = instance.getConfigUtils();

		if (isEnabled()) {
			final String section = "Events.AntiCraft";
			final String YMLFile = "AntiCraft.yml";

			configUtils.createConfig(YMLFile);
			storage = configUtils.getConfigFile(YMLFile);

			f = new File(instance.getDataFolder().getAbsolutePath(), YMLFile);

			Message = instance.getConfig().getString(section + ".MSG");
			bypass = instance.getConfig().getString(section + ".Bypass");
			AdminPerm = instance.getConfig().getString(section + ".AdminPerm");

			craftingEnabled   = instance.getConfig().getBoolean(section + ".Crafting", true);
			anvilEnabled      = instance.getConfig().getBoolean(section + ".Anvil", true);
			smithingEnabled   = instance.getConfig().getBoolean(section + ".Smithing", true);
			grindstoneEnabled = instance.getConfig().getBoolean(section + ".Grindstone", true);
			enchantingEnabled = instance.getConfig().getBoolean(section + ".Enchanting", true);

			bannedAnvil      = loadMaterials("BlockedAnvil");
			bannedSmithing   = loadMaterials("BlockedSmithing");
			bannedGrindstone = loadMaterials("BlockedGrindstone");
			bannedEnchanting = loadMaterials("BlockedEnchanting");

			rebuildCache();
		}
	}

	private Set<Material> loadMaterials(String path) {
		Set<Material> set = EnumSet.noneOf(Material.class);
		for (String name : storage.getStringList(path)) {
			Material m = Material.matchMaterial(name);
			if (m == null) {
				plugin.getLogger().warning("[AntiCraft] Invalid material '" + name + "' in " + path + " - skipping.");
				continue;
			}
			set.add(m);
		}
		return set;
	}

	private static void rebuildCache() {
		blockedCache.clear();
		List<String> raw = storage.getStringList("BlockedCrafting");
		if (raw == null) return;
		for (String s : raw) {
			ItemStack item = toItemStack(s);
			if (item == null) {
				// legacy/unknown material name (e.g. old 1.8 names like "web", "eye_of_ender")
				Util.consoleMSG("&e[AntiCraft] Skipping unknown material in AntiCraft.yml: " + s);
				continue;
			}
			blockedCache.computeIfAbsent(item.getType(), k -> new HashSet<>()).add(item.getDurability());
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPrepare(PrepareItemCraftEvent e) {
		if (!craftingEnabled) return;
		if (e.getRecipe() == null || e.getRecipe().getResult() == null) return;

		ItemStack item = e.getRecipe().getResult();
		if (isBlocked(item) && perms(item, e.getViewers())) {
			e.getInventory().setItem(0, null);
			notifyBlocked(e.getViewers(), item.getType(), "craft");
		}
	}

	// last block message shown to a viewer + when - the Prepare* events fire
	// repeatedly for one item placement, so we drop identical repeats within a window
	private final Map<java.util.UUID, String> lastNotify = new HashMap<>();
	private final Map<java.util.UUID, Long> lastNotifyAt = new HashMap<>();
	private static final long NOTIFY_WINDOW_MS = 1000;

	// tell every viewer the station action is blocked, reusing the shared MSG
	private void notifyBlocked(List<HumanEntity> viewers, Material type, String action) {
		if (Message == null || Message.isEmpty()) return;
		// legacy: old configs hardcoded ' craft ' instead of %action%, so swap the
		// bare word in when the placeholder is missing
		String template = Message.contains("%action%") ? Message : Message.replace(" craft ", " %action% ");
		String msg = Util.color(template
				.replace("%action%", action)
				.replace("%item%", type.toString().replace("_", " ").toLowerCase()));
		long now = System.currentTimeMillis();
		for (HumanEntity h : viewers) {
			java.util.UUID id = h.getUniqueId();
			Long at = lastNotifyAt.get(id);
			if (at != null && now - at < NOTIFY_WINDOW_MS && msg.equals(lastNotify.get(id))) continue;
			lastNotify.put(id, msg);
			lastNotifyAt.put(id, now);
			h.sendMessage(msg);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onClick(InventoryClickEvent e) {
		if (!craftingEnabled) return;
		if (e.getClickedInventory() == null || e.getClickedInventory().getType() != InventoryType.CRAFTING) return;

		ItemStack item = ((CraftingInventory) e.getClickedInventory()).getResult();
		if (item == null) return;

		if (isBlocked(item) && perms(item, e.getClickedInventory().getViewers())) {
			e.setCancelled(true);
			e.getInventory().setItem(0, null);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onAnvil(PrepareAnvilEvent e) {
		if (!anvilEnabled) return;
		ItemStack result = e.getResult();
		if (isStationBlocked(bannedAnvil, result)) {
			notifyBlocked(e.getViewers(), result.getType(), "use the anvil on");
			e.setResult(null);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onSmithing(PrepareSmithingEvent e) {
		if (!smithingEnabled) return;
		ItemStack result = e.getInventory().getResult();
		if (isStationBlocked(bannedSmithing, result)) {
			notifyBlocked(e.getViewers(), result.getType(), "smith");
			e.setResult(null);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onGrindstone(PrepareGrindstoneEvent e) {
		if (!grindstoneEnabled) return;
		ItemStack result = e.getResult();
		if (isStationBlocked(bannedGrindstone, result)) {
			notifyBlocked(e.getViewers(), result.getType(), "use the grindstone on");
			e.setResult(null);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEnchant(PrepareItemEnchantEvent e) {
		if (!enchantingEnabled) return;
		if (isStationBlocked(bannedEnchanting, e.getItem())) {
			notifyBlocked(e.getViewers(), e.getItem().getType(), "enchant");
			e.setCancelled(true);
		}
	}

	// package-private for tests: an output is blocked when its type is in the station's ban list
	static boolean isStationBlocked(Set<Material> banned, ItemStack result) {
		return result != null && isStationBlocked(banned, result.getType());
	}

	static boolean isStationBlocked(Set<Material> banned, Material type) {
		return type != null && banned.contains(type);
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 0) {
			sender.sendMessage(" ");
			sender.sendMessage(Util.color("&c&lAntiCraft"));
			sender.sendMessage(Util.color("&7Help, Block, Unblock"));
			sender.sendMessage(" ");
			return false;
		}

		if (!(sender instanceof Player)) {
			sender.sendMessage("Player only command.");
			return false;
		}

		if (!sender.hasPermission(AdminPerm)) {
			sender.sendMessage(Util.color("&cNot enough permissions."));
			return false;
		}

		if (args.length == 1) {
			ItemStack hand = ((Player) sender).getItemInHand();
			String msg = "";

			if (args[0].equalsIgnoreCase("block")) {
				if (isBlocked(hand)) {
					msg = "&c&lThat item is already blocked!";
				} else {
					add(hand);
					msg = "&c&lYou have blocked " + toConfigString(hand);
				}
			}

			if (args[0].equalsIgnoreCase("unblock")) {
				if (!isBlocked(hand)) {
					msg = "&c&lThat item is not blocked!";
				} else {
					remove(hand);
					msg = "&c&lYou have un-blocked " + toConfigString(hand);
				}
			}

			if (msg.length() > 1) {
				sender.sendMessage(" ");
				sender.sendMessage(Util.color(msg));
				sender.sendMessage(" ");
			} else {
				sender.sendMessage(" ");
				sender.sendMessage(Util.color("&e&l/" + label + " block &7- Block crafting of this item."));
				sender.sendMessage(Util.color("&e&l/" + label + " unblock &7- Allow crafting of this item."));
				sender.sendMessage(" ");
			}
			return false;
		}
		return false;
	}

	private static boolean isBlocked(ItemStack item) {
		Set<Short> durabilities = blockedCache.get(item.getType());
		return durabilities != null && durabilities.contains(item.getDurability());
	}

	private static void add(ItemStack item) {
		List<String> blocked = storage.getStringList("BlockedCrafting");
		if (blocked == null) blocked = new java.util.ArrayList<>();
		blocked.add(toConfigString(item));
		storage.set("BlockedCrafting", blocked);
		save();
		rebuildCache();
	}

	private static void remove(ItemStack item) {
		List<String> blocked = storage.getStringList("BlockedCrafting");
		if (blocked == null) blocked = new java.util.ArrayList<>();
		blocked.remove(toConfigString(item));
		storage.set("BlockedCrafting", blocked);
		save();
		rebuildCache();
	}

	private boolean perms(ItemStack item, List<HumanEntity> viewers) {
		String basePerm = bypass + item.getType().name().toLowerCase().replace("legacy_", "");
		String durabPerm = basePerm + ":" + item.getDurability();
		for (HumanEntity h : viewers) {
			if (item.getDurability() == 0 && h.hasPermission(basePerm)) return false;
			if (h.hasPermission(durabPerm)) return false;
		}
		return true;
	}

	private static void save() {
		try {
			storage.save(f);
		} catch (IOException ignored) {}
	}

	private static String toConfigString(ItemStack item) {
		return item.getType().name().toLowerCase(Locale.ENGLISH) + ((item.getDurability() != 0) ? ("-" + item.getDurability()) : "");
	}

	private static ItemStack toItemStack(String s) {
		s = s.toUpperCase();
		short data = 0;
		int dashIdx = s.indexOf('-');
		if (dashIdx >= 0) {
			String durStr = s.substring(dashIdx + 1);
			if (!durStr.isEmpty()) {
				try {
					data = Short.parseShort(durStr);
				} catch (NumberFormatException e) {
					return null; // malformed durability, e.g. "stone-abc"
				}
			}
			s = s.substring(0, dashIdx);
		}
		Material mat = Material.getMaterial(s);
		if (mat == null) return null;
		return new ItemStack(mat, 1, data);
	}
}
