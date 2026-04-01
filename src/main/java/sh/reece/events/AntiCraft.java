package sh.reece.events;

import java.io.File;
import java.io.IOException;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
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

			rebuildCache();
		}
	}

	private static void rebuildCache() {
		blockedCache.clear();
		List<String> raw = storage.getStringList("BlockedCrafting");
		if (raw == null) return;
		for (String s : raw) {
			ItemStack item = toItemStack(s);
			blockedCache.computeIfAbsent(item.getType(), k -> new HashSet<>()).add(item.getDurability());
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPrepare(PrepareItemCraftEvent e) {
		if (e.getRecipe() == null || e.getRecipe().getResult() == null) return;

		ItemStack item = e.getRecipe().getResult();
		if (isBlocked(item) && perms(item, e.getViewers())) {
			e.getInventory().setItem(0, null);
			if (Message != null && !Message.isEmpty()) {
				for (HumanEntity h : e.getViewers()) {
					h.sendMessage(Util.color(Message.replace("%item%", item.getType().toString().replace("_", " ").toLowerCase())));
				}
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onClick(InventoryClickEvent e) {
		if (e.getClickedInventory() == null || e.getClickedInventory().getType() != InventoryType.CRAFTING) return;

		ItemStack item = ((CraftingInventory) e.getClickedInventory()).getResult();
		if (item == null) return;

		if (isBlocked(item) && perms(item, e.getClickedInventory().getViewers())) {
			e.setCancelled(true);
			e.getInventory().setItem(0, null);
		}
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
			if (!durStr.isEmpty()) data = Short.parseShort(durStr);
			s = s.substring(0, dashIdx);
		}
		return new ItemStack(Material.getMaterial(s), 1, data);
	}
}
