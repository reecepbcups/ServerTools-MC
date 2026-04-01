package sh.reece.core;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class Trash extends BaseCommand {

	public Trash(Main instance) {
		super(instance, "Core.Trash", "trash");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player p = (Player) sender;
		p.openInventory(Bukkit.getServer().createInventory((InventoryHolder) p, 54, Util.color("&lTrash Bin")));

		return true;
	}
}
