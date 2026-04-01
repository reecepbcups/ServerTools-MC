package sh.reece.core;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class AdminChat extends BaseCommand {

	public AdminChat(Main instance) {
		super(instance, "Core.AdminChat", "adminchat");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (noPermission(sender, cmd)) return true;

		if (args.length == 0) {
			sender.sendMessage(Util.color("&fUsage: &c/" + label + " <message>"));
		} else if (args.length >= 1) {
			String adminChatMSG = configUtils.lang("ADMINCHAT")
					.replace("%player%", sender.getName())
					.replace("%msg%", Util.argsToSingleString(0, args));

			Bukkit.broadcast(adminChatMSG, permission);
			Util.consoleMSG(adminChatMSG);
		}

		return true;
	}
}
