package sh.reece.core;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Compass extends BaseCommand {

	public Compass(Main instance) {
		super(instance, "Core.Compass", "compass");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (noPermission(sender, cmd)) return true;

		Player p = (Player) sender;

		int bearing = (int) (p.getLocation().getYaw() + 180 + 360) % 360;
		String dir;
		if (bearing < 23) {
			dir = "North";
		} else if (bearing < 68) {
			dir = "North East";
		} else if (bearing < 113) {
			dir = "East";
		} else if (bearing < 158) {
			dir = "South East";
		} else if (bearing < 203) {
			dir = "South";
		} else if (bearing < 248) {
			dir = "South West";
		} else if (bearing < 293) {
			dir = "West";
		} else if (bearing < 338) {
			dir = "North West";
		} else {
			dir = "North";
		}

		Util.coloredMessage(p, configUtils.lang("COMPASS").replace("%dir%", dir).replace("%bearing%", bearing + ""));

		return true;
	}
}
