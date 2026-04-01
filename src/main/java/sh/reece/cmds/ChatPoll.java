package sh.reece.cmds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class ChatPoll extends BaseCommand implements Listener {

	//private HashMap<Player, String> v = new HashMap<Player, String>();
	private List<String> votes = new ArrayList<String>();
	private List<Player> playersVoted = new ArrayList<Player>();
	private Boolean pollRunning;
	private int options; // numbers which can be used

	public ChatPoll(Main instance) {
		super(instance, "Commands.ChatPoll", "poll");

		if(isEnabled()) {
			pollRunning = false;
		}
	}

	public static <T> T mostCommon(List<T> list) {
	    Map<T, Integer> map = new HashMap<>();
	    for (T t : list) {
	        Integer val = map.get(t);
	        map.put(t, val == null ? 1 : val + 1);
	    }

	    Entry<T, Integer> max = null;
	    for (Entry<T, Integer> e : map.entrySet()) {
	        if (max == null || e.getValue() > max.getValue())
	            max = e;
	    }
	    return max != null ? max.getKey() : null;
	}

	@EventHandler(ignoreCancelled = true)
	public void playerColoredChatEvent(AsyncPlayerChatEvent e) {
		Player p = e.getPlayer();
		String voteVal = e.getMessage().split(" ")[0].replace("#", "");


		if(pollRunning == false) {return;}

		// if poll running but not a number, do nothing
		if(Util.isInt(voteVal) == false) {return;}

		// if they already voted
		if(playersVoted.contains(p)) {
			Util.coloredMessage(p, " \n&cYou already voted!\n&7&o(( You can not type more numbers in chat ))\n ");
			e.setCancelled(true);
			return;
		}

		if(options-Integer.valueOf(voteVal)>=0) {
			Util.coloredMessage(p, " \n&a[!] &aRegistered your poll vote &n#"+voteVal+"&a!\n ");
			playersVoted.add(p);  votes.add(voteVal);

		} else {
			Util.coloredMessage(p, " \n&c[!] Incorrect Value: &n"+voteVal+"&c not in range #1-"+options+"\n ");
		}
		e.setCancelled(true);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender.hasPermission("poll.use"))) {
			sender.sendMessage(Util.color("&cNo Permission to use "+label+" :("));
			return true;
		}

		Player p = (Player) sender;

		if (args.length == 0) {
			sendHelpMenu(p);
			return true;
		}

		switch(args[0]){
		// /poll start #
		case "start":
		case "begin":
			if(args.length >= 2) {
				pollRunning = true;
				options = Integer.valueOf(args[1]);
				Util.coloredBroadcast(" \n&f&lPOLL &fEnter your choice &n#1 -> #" + options + "&f in chat\n ");
			} else {
				sendHelpMenu(p);
			}
			return true;

		case "stop":
		case "end":
			if(pollRunning == false) {
				Util.coloredMessage(p, "&cNo poll is running!");
			} else {
				pollRunning = false;
				if(votes.isEmpty()) {
					Util.coloredBroadcast("&c&lPOLL ended with no votes!");
				} else {
					Util.coloredBroadcast("&a&lPOLL WINNER &a&n#" + mostCommon(votes));
				}
			}

			return true;
		default:
			sendHelpMenu(p);
			return true;
		}
	}

	public void sendHelpMenu(Player p) {
		Util.coloredMessage(p, "&f/poll &7start <number (#1-10)>");
		Util.coloredMessage(p, "&f/poll &7stop");
	}



}
