package sh.reece.cmds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import me.clip.placeholderapi.PlaceholderAPI;
import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class FancyAnnounce extends BaseCommand {

	private FileConfiguration config;

	public FancyAnnounce(Main instance) {
	    super(instance, "FancyAnnounce", "announce");

	    if (isEnabled()) {
	    	config = plugin.getConfig();
	    }
	}

	private static List<String> possibleArugments = new ArrayList<String>();
	private static List<String> result = new ArrayList<String>();
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {

		if(possibleArugments.isEmpty()) {
			for(String group : config.getConfigurationSection("FancyAnnounce.Groups").getKeys(false)) {
				possibleArugments.add(group);
			}
		}
		result.clear();
		if(args.length == 1) {
			for(String a : possibleArugments) {
				if(a.toLowerCase().startsWith(args[0].toLowerCase())) {
					result.add(a);
				}
			}
			return result;
		}
		return null;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (noPermission(sender, cmd)) return true;

		// if no arguments given
		if((args.length < 1)) {
			sender.sendMessage(Util.color("&c/Announce <Type>"));
			sender.sendMessage("Types: " + config.getConfigurationSection("FancyAnnounce.Groups").getKeys(false).toString());
			return true;
		}

		String Section = "FancyAnnounce.Groups." + args[0];
		String cmd_syntax = config.getString(Section + ".ArgumentsSyntax");

		if (!(config.getStringList(Section) != null)) {
			sender.sendMessage(Util.color("&cThe section \"" + Section + "\" could not be found D:"));
			return true;
		}

		int syntaxCount = cmd_syntax.split(" ").length;
		if(args.length < syntaxCount + 1) {
			sender.sendMessage("Usage: /" + cmd.getName() + " " + args[0] + " " + cmd_syntax);
			return true;
		}

		// build each %arg-N% value up front. the last declared argument absorbs
		// every remaining word, so <Location> can be "The Nether Fortress"
		String[] argValues = new String[syntaxCount + 1];
		for (int i = 0; i <= syntaxCount; i++) {
			if (i == syntaxCount) {
				argValues[i] = String.join(" ", Arrays.copyOfRange(args, i, args.length));
			} else {
				argValues[i] = args[i];
			}
		}

		for (String str : config.getStringList(Section + ".Message")) {

			for(int i = 0; i <= syntaxCount; i++) {
				str = str.replace("%arg-" + i + "%", argValues[i]);
			}
			str = str.replace("%player%", sender.getName());

			if(config.getString(Section + ".CenterMessage").equalsIgnoreCase("true")) {
				for (Player all : Bukkit.getOnlinePlayers()) {
					Util.sendCenteredMessage(all, applyPlaceholders(all, str));
				}
			} else {
				Player ref = (sender instanceof Player) ? (Player) sender : Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
				Bukkit.broadcastMessage(applyPlaceholders(ref, str));
			}


		}

		return true;
	}

	// non-player-specific placeholders (e.g. %stools_age_...%) still resolve fine with any online player
	private String applyPlaceholders(Player player, String str) {
		if (player != null && Main.isPAPIEnabled() && str.contains("%")) {
			return PlaceholderAPI.setPlaceholders(player, str);
		}
		return str;
	}
}
