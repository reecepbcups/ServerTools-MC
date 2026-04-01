package sh.reece.core;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Enchant extends BaseCommand {

	public Enchant(Main instance) {
		super(instance, "Core.Enchant", "enchant");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			Util.coloredMessage(sender, "&c[!] Only players can enchant items!");
			return true;
		}

		Player p = (Player) sender;

		if (noPermission(sender, cmd)) return true;

		if (args.length == 0) {
			Util.coloredMessage(sender, "&c[!] Usage: &7/enchant <enchantment> [level]");
			return true;
		}

		if (args.length >= 1) {
			ItemStack is = p.getInventory().getItem(p.getInventory().getHeldItemSlot());
			Util.consoleMSG(is.getType() + " " + is.getItemMeta().getDisplayName());

			String enchantName = args[0].toUpperCase();
			if (checkValid(enchantName, p, is)) {
				int level = 1;
				if (args.length == 2) { level = Integer.parseInt(args[1]); }

				is.addUnsafeEnchantment(Enchantment.getByName(enchantName), level);
			}

			return true;
		}

		return true;
	}

	private boolean checkValid(String enchantName, Player p, ItemStack is) {
		Util.consoleMSG("Valid Enchant Check");

		if (!(Enchantment.getByName(enchantName) != null)) {
			Util.coloredMessage(p, "&c[!] &7" + enchantName + " &cis not a valid enchantment!");
			return false;
		}

		if (is == null || is.getType() == Material.AIR) {
			Util.coloredMessage(p, "&c[!] &7You need to hold an item in your hand!");
			return false;
		}

		return true;
	}

}
