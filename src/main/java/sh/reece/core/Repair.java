package sh.reece.core;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.TimeUtil;
import sh.reece.utiltools.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;

public class Repair extends BaseCommand {

	// Per rank cost/cooldown lives in the permission node itself, e.g.
	// servertools.repair.all.cost.5000 or servertools.repair.single.cooldown.15m.
	// A player with several of them gets the cheapest cost and the shortest cooldown.
	private static final String PERM_PREFIX = "servertools.repair.";
	private static final String SINGLE = "single";
	private static final String ALL = "all";

	private String SingleRepairPerm, AllRepairPerm;
	private double SingleRepairCost, AllRepairCost;
	private int SingleRepairCooldown, AllRepairCooldown;
	private final Map<String, Long> singleCooldowns = new HashMap<>();
	private final Map<String, Long> allCooldowns = new HashMap<>();
	private static Economy econ = null;

	public Repair(Main instance) {
		super(instance, "Core.Repair", "repair", "fix");
		if (isEnabled()) {
			SingleRepairPerm = plugin.getConfig().getString(section + ".Single.Permission");
			AllRepairPerm = plugin.getConfig().getString(section + ".All.Permission");

			SingleRepairCooldown = configuredCooldown(".Single.Cooldown");
			AllRepairCooldown = configuredCooldown(".All.Cooldown");

			setupEco();
			if (econ != null) {
				SingleRepairCost = plugin.getConfig().getDouble(section + ".Single.Cost");
				AllRepairCost = plugin.getConfig().getDouble(section + ".All.Cost");
			} else {
				SingleRepairCost = 0;
				AllRepairCost = 0;
			}
		}
	}

	private int configuredCooldown(String path) {
		final String raw = plugin.getConfig().getString(section + path, "0");
		final int seconds = TimeUtil.parseDuration(raw);
		if (seconds < 0) {
			Util.consoleMSG("&c[Repair] Bad cooldown '" + raw + "' at " + section + path + ", using none.");
			return 0;
		}
		return seconds;
	}

	private boolean setupEco() {
		if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = (Economy) rsp.getProvider();
		return (econ != null);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			Util.coloredMessage(sender, "&c[!] Only players can fix items!");
			return true;
		}

		Player p = (Player) sender;

		if (p.getInventory().getContents().length <= 0) {
			Util.coloredMessage(p, "&c[!] You don't have any items to repair!");
			return true;
		}

		if (args.length == 0) {
			if (!p.hasPermission(SingleRepairPerm)) {
				Util.coloredMessage(p, "&c[!] You don't have permission to repair items!");
				return true;
			}

			final int cooldown = resolveCooldown(p, SINGLE, SingleRepairCooldown);
			if (onCooldown(p, singleCooldowns, cooldown, "&c[!] You must wait &f%time% &cbefore repairing again!")) {
				return true;
			}
			if (repairItem(p, p.getInventory().getItemInHand(), true)) {
				Util.startCooldown(singleCooldowns, cooldown, p.getName());
			}

		} else if (args.length == 1) {
			if (args[0].equalsIgnoreCase("all")) {
				if (p.hasPermission(AllRepairPerm)) {
					repairAllItems(p);
					return true;
				}
				Util.coloredMessage(p, "&c[!] You don't have permission to repair all items!");

			} else if (args[0].equalsIgnoreCase("hand")) {
				p.performCommand("repair");
			} else {
				Util.coloredMessage(p, "&fUsage: &c/" + label + " [all / hand]");
			}
		}

		return true;
	}

	private boolean repairItem(Player p, ItemStack item, boolean SingleItemFix) {
		if (SingleItemFix && item == null || item.getType() == Material.AIR) {
			Util.coloredMessage(p, "&c[!] You need to hold an item in your hand!");
			return false;
		}

		final String[] TYPE_CONTENT = {"HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS", "SWORD", "AXE", "PICKAXE", "SHOVEL", "HOE", "BOW"};
		boolean canBeRepaired = false;
		for (final String type : TYPE_CONTENT) {
			if (item.getType().name().contains(type)) {
				canBeRepaired = true;
				Util.consoleMSG(item.getType().name() + " is a " + type);
				break;
			}
		}

		if (item.getType().getMaxDurability() == 0 || canBeRepaired == false) {
			if (SingleItemFix) {
				Util.coloredMessage(p, "&c[!] This item cannot be repaired!");
			}
			return false;
		}

		if (SingleItemFix && item.getDurability() == 0) {
			Util.coloredMessage(p, "&c[!] This item is already fully repaired!");
			return false;
		}

		if (SingleItemFix && item.getEnchantments().size() > 0) {
			if (!p.hasPermission("repair.enchanted")) {
				Util.coloredMessage(p, "&c[!] You cannot repair enchanted items!");
				return false;
			}
		}

		if (SingleItemFix) {
			final double cost = resolveCost(p, SINGLE, SingleRepairCost);
			if (!chargePlayer(p, cost, "&c[!] You don't have enough money to repair this item &7((" + cost + ")) &f!")) {
				return false;
			}
			Util.coloredMessage(p, "&a[!] You repaired your %item% for &6%cost%&a!"
					.replace("%item%", fmtName(item))
					.replace("%cost%", cost + ""));
		}

		item.setDurability((short) 0);
		if (!SingleItemFix) {
			Util.coloredMessage(p, "&a[!] You repaired your %item%!".replace("%item%", fmtName(item)));
		}
		return true;
	}

	private void repairAllItems(Player p) {
		final int cooldown = resolveCooldown(p, ALL, AllRepairCooldown);
		if (onCooldown(p, allCooldowns, cooldown, "&c[!] You must wait &f%time% &cbefore repairing everything again!")) {
			return;
		}

		final double cost = resolveCost(p, ALL, AllRepairCost);
		if (chargePlayer(p, cost, "&c[!] You don't have enough money to repair all items &7((%cost%)) &f!".replace("%cost%", cost + ""))) {
			RepairItems(p, p.getInventory().getContents());
			RepairItems(p, p.getInventory().getArmorContents());
			Util.coloredMessage(p, "&a[!] You repaired the above for &6%cost%&a!".replace("%cost%", cost + ""));
			Util.startCooldown(allCooldowns, cooldown, p.getName());
		}
	}

	private void RepairItems(Player p, ItemStack[] items) {
		for (ItemStack item : items) {
			if (item != null && item.getType() != Material.AIR) {
				repairItem(p, item, false);
			}
		}
	}

	private boolean onCooldown(Player p, Map<String, Long> cooldowns, int cooldown, String msg) {
		if (cooldown <= 0) {
			return false;
		}

		final long left = Util.cooldownSecondsLeft(cooldowns, 0, p.getName());
		if (left <= 0) {
			return false;
		}
		Util.coloredMessage(p, msg.replace("%time%", TimeUtil.formatDuration(left)));
		return true;
	}

	/** Suffixes of every servertools.repair.<mode>.<key>.* node the player actually has. */
	private List<String> permValues(Player p, String mode, String key) {
		final String prefix = PERM_PREFIX + mode + "." + key + ".";
		final List<String> values = new ArrayList<>();
		for (PermissionAttachmentInfo info : p.getEffectivePermissions()) {
			final String node = info.getPermission().toLowerCase();
			if (info.getValue() && node.startsWith(prefix)) {
				values.add(node.substring(prefix.length()));
			}
		}
		return values;
	}

	/** Ops and anyone holding * repair for free, with no cooldown. */
	private boolean bypasses(Player p) {
		return p.isOp() || p.hasPermission("*");
	}

	/** Cheapest cost the player's permissions grant, or the config default. */
	private double resolveCost(Player p, String mode, double fallback) {
		if (bypasses(p)) return 0;

		double cost = fallback;
		for (String raw : permValues(p, mode, "cost")) {
			try {
				cost = Math.min(cost, Double.parseDouble(raw));
			} catch (NumberFormatException ignored) {
				Util.consoleMSG("&c[Repair] Bad cost in permission " + PERM_PREFIX + mode + ".cost." + raw);
			}
		}
		return Math.max(cost, 0);
	}

	/** Shortest cooldown the player's permissions grant, or the config default. */
	private int resolveCooldown(Player p, String mode, int fallback) {
		if (bypasses(p)) return 0;

		int cooldown = fallback;
		for (String raw : permValues(p, mode, "cooldown")) {
			final int seconds = TimeUtil.parseDuration(raw);
			if (seconds < 0) {
				Util.consoleMSG("&c[Repair] Bad cooldown in permission " + PERM_PREFIX + mode + ".cooldown." + raw);
				continue;
			}
			cooldown = Math.min(cooldown, seconds);
		}
		return cooldown;
	}

	private boolean chargePlayer(Player p, double cost, String msg) {
		if (econ != null && cost > 0) {
			if (econ.getBalance(p) <= cost) {
				Util.coloredMessage(p, msg);
				return false;
			}
			econ.withdrawPlayer(p, cost);
		}
		return true;
	}

	private String fmtName(ItemStack item) {
		return item.getType().toString().replace("LEGACY", "").replace("_", " ").toLowerCase();
	}
}
