package sh.reece.core;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Realname extends BaseCommand {

	public Realname(Main instance) {
		super(instance, "Core.Realname", "realname");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (noPermission(sender, cmd)) return true;

		if (args.length == 0) {
			sender.sendMessage(Util.color("&fUsage: &c/" + label + " <player>"));

		} else if (args.length >= 1) {

			Player target = Bukkit.getPlayer(args[0]);

			if (target == null) {
				Util.coloredMessage(sender, "&c" + args[0] + " &fis not online!");

			} else {
				Util.coloredMessage(sender, "User " + args[0] + " real name is " + target.getName());
			}
		}

		return false;
	}
}
