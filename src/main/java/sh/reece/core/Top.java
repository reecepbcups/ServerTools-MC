package sh.reece.core;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Top extends BaseCommand {

	public Top(Main instance) {
		super(instance, "Core.Top", "top");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (noPermission(sender, cmd)) return true;

		Player p = (Player) sender;
		Block block = p.getWorld().getHighestBlockAt(p.getLocation());
		p.teleport(block.getLocation().add(0, 1, 0));
		Util.coloredMessage(p, configUtils.lang("TOP_TELEPORT").replace("%block%", block.getY() + ""));

		return true;
	}
}
