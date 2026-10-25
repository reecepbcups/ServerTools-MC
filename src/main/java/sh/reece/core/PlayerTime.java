package sh.reece.core;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Schedulers;
import sh.reece.utiltools.TimeUtil;
import sh.reece.utiltools.Util;

// /ptime: a time of day only this player sees. Stays put until reset, doesn't survive a relog.
public class PlayerTime extends BaseCommand {

	private static final List<String> OPTIONS = List.of("day", "noon", "sunset", "night", "midnight", "sunrise", "reset");

	private final String othersPerm;

	public PlayerTime(Main instance) {
		super(instance, "Core.PlayerTime", "ptime");
		othersPerm = plugin.getConfig().getString(section + ".OthersPerm", "essentials.ptime.others");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (noPermission(sender, cmd)) return true;

		if (args.length == 0) {
			Player p = playerOrNull(sender);
			if (p == null) return true;
			Util.coloredMessage(p, p.isPlayerTimeRelative()
				? "&fYour time follows the server. &7/" + label + " <day/night/12:00/reset>"
				: "&fYour time is fixed at &e" + TimeUtil.formatWorldTime(p.getPlayerTime()) + "&f. &7/" + label + " reset");
			return true;
		}

		Player target;
		if (args.length >= 2) {
			if (!hasPermission(sender, othersPerm)) {
				Util.coloredMessage(sender, "&cYou do not have access to &n/" + label + "&c for others.");
				return true;
			}
			target = Bukkit.getPlayerExact(args[1]);
			if (target == null) {
				Util.coloredMessage(sender, "&c" + args[1] + " &fis not online!");
				return true;
			}
		} else {
			target = playerOrNull(sender);
			if (target == null) return true;
		}

		// @ marks a fixed time in other plugins; fixed is the only mode here
		String value = args[0].startsWith("@") ? args[0].substring(1) : args[0];
		String result;
		if (value.equalsIgnoreCase("reset") || value.equalsIgnoreCase("normal")) {
			Schedulers.entity(plugin, target, target::resetPlayerTime);
			result = "back to the server's time";
		} else {
			long ticks = TimeUtil.parseWorldTime(value);
			if (ticks < 0) {
				Util.coloredMessage(sender, "&cUsage: &f/" + label + " <day/night/noon/midnight/12:00/ticks/reset> [player]");
				return true;
			}
			Schedulers.entity(plugin, target, () -> target.setPlayerTime(ticks, false));
			result = "fixed at &e" + TimeUtil.formatWorldTime(ticks);
		}

		if (target != sender) {
			Util.coloredMessage(sender, "&a" + target.getName() + "'s time is now " + result + "&a.");
		}
		Util.coloredMessage(target, "&aYour time is now " + result + "&a.");
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
		if (args.length == 1) {
			List<String> out = new ArrayList<>();
			for (String o : OPTIONS) {
				if (o.startsWith(args[0].toLowerCase())) out.add(o);
			}
			return out;
		}
		if (args.length == 2 && hasPermission(sender, othersPerm)) {
			return super.onTabComplete(sender, cmd, alias, new String[]{args[1]});
		}
		return List.of();
	}
}
