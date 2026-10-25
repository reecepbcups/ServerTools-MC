package sh.reece.core;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Schedulers;
import sh.reece.utiltools.TimeUtil;
import sh.reece.utiltools.Util;

// /time [day|12:00|set <time>|add <ticks>] [world|all]. No world means the sender's world, or every world from console.
public class WorldTime extends BaseCommand {

	private static final List<String> OPTIONS = List.of("day", "noon", "sunset", "night", "midnight", "sunrise", "set", "add");

	private final String setPerm;

	public WorldTime(Main instance) {
		super(instance, "Core.Time", "time");
		setPerm = plugin.getConfig().getString(section + ".SetPerm", "essentials.time.set");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (noPermission(sender, cmd)) return true;

		// /day and /night are shortcuts for /time day and /time night, any args name the world
		if (label.equalsIgnoreCase("day") || label.equalsIgnoreCase("night")) {
			String[] shifted = new String[args.length + 1];
			shifted[0] = label.toLowerCase();
			System.arraycopy(args, 0, shifted, 1, args.length);
			args = shifted;
		}

		if (args.length == 0) {
			for (World world : Bukkit.getWorlds()) {
				Util.coloredMessage(sender, "&f" + world.getName() + "&7: &e" + TimeUtil.formatWorldTime(world.getTime())
					+ " &7(" + world.getTime() + " ticks, day " + world.getFullTime() / 24000 + ")");
			}
			return true;
		}

		if (!hasPermission(sender, setPerm)) {
			Util.coloredMessage(sender, "&cYou do not have access to change the time.");
			return true;
		}

		// vanilla style "set <time>" and "add <ticks>" work too
		boolean add = args[0].equalsIgnoreCase("add");
		int valueIndex = add || args[0].equalsIgnoreCase("set") ? 1 : 0;
		if (args.length <= valueIndex) {
			sendUsage(sender, label);
			return true;
		}

		long ticks = TimeUtil.parseWorldTime(args[valueIndex]);
		if (add) {
			ticks = Util.isInt(args[valueIndex]) ? Long.parseLong(args[valueIndex]) : -1;
		}
		if (ticks < 0) {
			sendUsage(sender, label);
			return true;
		}

		List<World> worlds = targetWorlds(sender, args.length > valueIndex + 1 ? args[valueIndex + 1] : null);
		if (worlds.isEmpty()) {
			Util.coloredMessage(sender, "&cNo world named &f" + args[valueIndex + 1] + "&c.");
			return true;
		}

		final long value = ticks;
		for (World world : worlds) {
			// world time belongs to the global region on Folia
			Schedulers.global(plugin, () -> {
				if (add) {
					world.setFullTime(world.getFullTime() + value);
				} else {
					world.setTime(value);
				}
			});
		}

		String what = add ? "Added &e" + value + " &aticks to" : "Set the time to &e" + TimeUtil.formatWorldTime(value) + " &ain";
		Util.coloredMessage(sender, "&a" + what + " &f" + names(worlds) + "&a.");
		return true;
	}

	private List<World> targetWorlds(CommandSender sender, String name) {
		if (name == null) {
			return sender instanceof Player p ? List.of(p.getWorld()) : Bukkit.getWorlds();
		}
		if (name.equalsIgnoreCase("all") || name.equals("*")) {
			return Bukkit.getWorlds();
		}
		World world = Bukkit.getWorld(name);
		return world == null ? List.of() : List.of(world);
	}

	private static String names(List<World> worlds) {
		List<String> names = new ArrayList<>(worlds.size());
		for (World w : worlds) names.add(w.getName());
		return String.join(", ", names);
	}

	private static void sendUsage(CommandSender sender, String label) {
		Util.coloredMessage(sender, "&cUsage: &f/" + label + " <day/night/noon/midnight/12:00/ticks> [world/all]");
		Util.coloredMessage(sender, "&f/" + label + " add <ticks> [world/all]");
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
		List<String> out = new ArrayList<>();
		if (!hasPermission(sender, setPerm)) return out;

		if (args.length == 1) {
			for (String o : OPTIONS) {
				if (o.startsWith(args[0].toLowerCase())) out.add(o);
			}
		} else if (args.length == 2 || args.length == 3) {
			String prefix = args[args.length - 1].toLowerCase();
			for (World w : Bukkit.getWorlds()) {
				if (w.getName().toLowerCase().startsWith(prefix)) out.add(w.getName());
			}
			if ("all".startsWith(prefix)) out.add("all");
		}
		return out;
	}
}
