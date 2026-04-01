package sh.reece.core;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Extinguish extends BaseCommand {

	private String StaffPermission;

	public Extinguish(Main instance) {
		super(instance, "Core.Extinguish", "extinguish");
		if (isEnabled()) {
			StaffPermission = plugin.getConfig().getString(section + ".StaffPermission");
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (noPermission(sender, cmd)) return true;

		Player target = (Player) sender;

		// if player tries to ext someone else & is a staff
		if (args.length > 0 && target.hasPermission(StaffPermission)) {
			if (Bukkit.getPlayer(args[0]) != null) {
				target = Bukkit.getPlayer(args[0]);
			} else {
				Util.coloredMessage(sender, "&cPlayer &n" + args[0] + "&c is not online.");
				return true;
			}
		}

		// remove fire from the player
		target.setFireTicks(0);
		Util.coloredMessage(target, "&a[+] You have been extinguished!");

		if (!target.getName().equalsIgnoreCase(sender.getName())) {
			Util.coloredMessage(sender, "&a[+] Successfully Extinguished &n" + target.getName());
		}

		return true;
	}
}
