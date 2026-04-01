package sh.reece.core;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class Broadcast extends BaseCommand {

	public Broadcast(Main instance) {
		super(instance, "Core.Broadcast", "broadcast");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (noPermission(sender, cmd)) return true;

		if (args.length == 0) {
			sender.sendMessage(Util.color("&fUsage: &c/" + label + " <message>"));
			return true;
		}

		String broadcastMSG = configUtils.lang("BROADCAST").replace("%msg%", Util.argsToSingleString(0, args));
		Util.coloredBroadcast(broadcastMSG);

		return true;
	}
}
