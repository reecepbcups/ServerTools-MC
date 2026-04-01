package sh.reece.core;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;

public class Repair extends BaseCommand {

	private String SingleRepairPerm, AllRepairPerm;
	private double SingleRepairCost, AllRepairCost;
	private static Economy econ = null;

	public Repair(Main instance) {
		super(instance, "Core.Repair", "repair", "fix");
		if (isEnabled()) {
			SingleRepairPerm = plugin.getConfig().getString(section + ".Single.Permission");
			AllRepairPerm = plugin.getConfig().getString(section + ".All.Permission");

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
			repairItem(p, p.getInventory().getItemInHand(), true);

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

	private void repairItem(Player p, ItemStack item, boolean SingleItemFix) {
		if (SingleItemFix && item == null || item.getType() == Material.AIR) {
			Util.coloredMessage(p, "&c[!] You need to hold an item in your hand!");
			return;
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
			return;
		}

		if (SingleItemFix && item.getDurability() == 0) {
			Util.coloredMessage(p, "&c[!] This item is already fully repaired!");
			return;
		}

		if (SingleItemFix && item.getEnchantments().size() > 0) {
			if (!p.hasPermission("repair.enchanted")) {
				Util.coloredMessage(p, "&c[!] You cannot repair enchanted items!");
				return;
			}
		}

		if (SingleItemFix) {
			if (chargePlayer(p, SingleRepairCost, "&c[!] You don't have enough money to repair this item &7((" + SingleRepairCost + ")) &f!")) {
				Util.coloredMessage(p, "&a[!] You repaired your %item% for &6" + SingleRepairCost + "&a!".replace("%item%", fmtName(item)));
				item.setDurability((short) 0);
			}
		} else {
			item.setDurability((short) 0);
			Util.coloredMessage(p, "&a[!] You repaired your %item%!".replace("%item%", fmtName(item)));
		}
	}

	private void repairAllItems(Player p) {
		if (chargePlayer(p, AllRepairCost, "&c[!] You don't have enough money to repair all items &7((%cost%)) &f!".replace("%cost%", AllRepairCost + ""))) {
			RepairItems(p, p.getInventory().getContents());
			RepairItems(p, p.getInventory().getArmorContents());
			Util.coloredMessage(p, "&a[!] You repaired the above for &6%cost%&a!".replace("%cost%", AllRepairCost + ""));
		}
	}

	private void RepairItems(Player p, ItemStack[] items) {
		for (ItemStack item : items) {
			if (item != null && item.getType() != Material.AIR) {
				repairItem(p, item, false);
			}
		}
	}

	private boolean chargePlayer(Player p, double cost, String msg) {
		if (econ != null) {
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
