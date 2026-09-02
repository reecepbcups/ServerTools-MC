package sh.reece.cmds;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import sh.reece.hoppers.HopperOptimizer;
import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;

/** /hopperopt - report the experimental hopper optimizer's status and counters. */
public class HopperOptCmd extends BaseCommand {

	public HopperOptCmd(Main instance) {
		super(instance, "Hoppers.Optimize", "hopperopt");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (noPermission(sender, cmd)) {
			return true;
		}
		sender.sendMessage(HopperOptimizer.statusText());
		return true;
	}
}
