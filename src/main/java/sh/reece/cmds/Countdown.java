package sh.reece.cmds;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Schedulers;
import sh.reece.utiltools.Util;

public class Countdown extends BaseCommand {

	private Integer count;
	private ScheduledTask task;

	public Countdown(Main instance) {
        super(instance, "Commands.Countdown", "countdown");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (noPermission(sender, cmd)) return true;

		if(args.length == 0) {
			sender.sendMessage(Util.color("&c/coutndown [seconds] [Reason For Countdown]"));
		} else {

			if(args.length == 1) {
				if(Util.isInt(args[0])) {
					runnable(Integer.valueOf(args[0]));
				}
			} else {
				if(Util.isInt(args[0])) {
					Util.coloredBroadcast(plugin.PREFIX+Util.argsToSingleString(1, args));
					runnable(Integer.valueOf(args[0]));
				} else {
					sender.sendMessage(Util.color("&c/coutndown [seconds] [Reason For Countdown]"));
				}
			}

		}
		return true;
	}

	String color = "&f&l";
	public void runnable(int start) {
		count = start;
		task = Schedulers.globalTimer(plugin, () -> {
			if(count==3) { color="&c&l"; }
			else if(count==2) { color="&e&l"; }
			else if(count==1) { color="&a&l"; }

			if(count<=1 && task != null) {
				task.cancel();
			}

			Util.coloredBroadcast(plugin.PREFIX+color+ count);
			count--;
		}, 0, 20L);
	}
}
