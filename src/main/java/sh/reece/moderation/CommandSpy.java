package sh.reece.moderation;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class CommandSpy extends BaseCommand implements Listener {

	private Set<String> ignored;

	// UUID set for PAPI lookups
	private static Set<UUID> watchingUUIDs;
	// Player set to iterate only watchers, not all online players.
	// CopyOnWriteArraySet is fine here: writes are rare (staff toggle), reads are frequent (every command).
	private static Set<Player> watchingPlayers;


	public CommandSpy(Main instance) {
		super(instance, "Moderation.CommandSpy", "commandspy");

		if(isEnabled()) {
			ignored = new HashSet<>(instance.getConfig().getStringList(section+".Ignored-ignored_commands"));
			watchingUUIDs = new HashSet<>();
			watchingPlayers = new CopyOnWriteArraySet<>();
		}
	}

	public static boolean isWatching(UUID uuid) {
		return watchingUUIDs.contains(uuid);
	}

	@EventHandler(ignoreCancelled = true)
	public void playerCommandSpyEvent(PlayerCommandPreprocessEvent e) {
		if (watchingPlayers.isEmpty()) return;
		if (e.getPlayer().hasPermission("commandspy.exempt")) return;

		String m = e.getMessage().toLowerCase();
		int si = m.indexOf(' ');
		String firstWord = si == -1 ? m : m.substring(0, si);
		if (ignored.contains(firstWord) || ignored.contains(m)) return;

		UUID senderUUID = e.getPlayer().getUniqueId();
		String n = e.getPlayer().getName();
		String msg = e.getMessage();
		String formatted = Util.color("&7CMDSPY &f&n" + n + "&8> &f " + msg);

		for (Player watcher : watchingPlayers) {
			if (!watcher.getUniqueId().equals(senderUUID)) {
				watcher.sendMessage(formatted);
			}
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		Player p = e.getPlayer();
		if (watchingUUIDs.remove(p.getUniqueId())) {
			watchingPlayers.remove(p);
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (noPermission(sender, cmd)) {
			return true;
		}

		Player p = (Player) sender;

		if (args.length == 0) {
			sendHelpMenu(p);
			return true;
		}

		switch(args[0].toLowerCase()){
		case "enable":
		case "e":
		case "start":
			if(watchingUUIDs.contains(p.getUniqueId())) {
				p.sendMessage("You already have this on");
			} else {
				Util.coloredMessage(p, "&7Commandspy has been &aenabled&7.");
				watchingUUIDs.add(p.getUniqueId());
				watchingPlayers.add(p);
			}
			return true;

		case "disable":
		case "d":
		case "stop":
			Util.coloredMessage(p, "&7Commandspy has been &cdisabled&7.");
			if(watchingUUIDs.remove(p.getUniqueId())) {
				watchingPlayers.remove(p);
			}
			return true;
		default:
			sendHelpMenu(p);
			return true;
		}
	}

	public void sendHelpMenu(Player p) {
		Util.coloredMessage(p, "&f/commandspy &7enable/disable");
	}
}
