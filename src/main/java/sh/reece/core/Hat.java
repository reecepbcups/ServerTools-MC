package sh.reece.core;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Hat extends BaseCommand {

	public static final String NO_ITEM = "<red>[!] You can not set your hat as nothing!";
	public static final String HAT_SET = "<green>[+] Hat has been set!";

	public Hat(Main instance) {
		super(instance, "Core.Hat", "hat");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (noPermission(sender, cmd)) return true;

		Player player = (Player) sender;

		int slot = player.getInventory().getHeldItemSlot();
		org.bukkit.inventory.ItemStack item = player.getInventory().getItem(slot);

		ItemStack helmet = player.getInventory().getHelmet();

		if (item == null || item.getType() == Material.AIR) {
			sender.sendRichMessage(NO_ITEM);
			return true;

		} else if (helmet != null) {
			player.getInventory().setHelmet(item);
			player.getInventory().setItem(slot, helmet);

		} else {
			player.getInventory().setHelmet(item);
			player.getInventory().setItem(slot, null);
		}

		sender.sendRichMessage(HAT_SET);

		return true;
	}
}
