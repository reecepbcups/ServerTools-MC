package sh.reece.core;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;

public class Workbench extends BaseCommand {

	public Workbench(Main instance) {
		super(instance, "Core.Workbench", "workbench", "craft");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (noPermission(sender, cmd)) return true;

		((Player) sender).openWorkbench(null, true);
		return true;
	}
}
