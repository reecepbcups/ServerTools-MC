package sh.reece.cmds;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Schedulers;
import sh.reece.utiltools.Util;

// minimal vanish: hides the sender from everyone lacking `permission`, and hides
// newly joining players who lack it from anyone currently vanished. Does not touch
// join/quit messages, mob AI, or item drops - those are left to a dedicated plugin
// if needed.
public class Vanish extends BaseCommand implements Listener {

	private static final String METADATA_KEY = "vanished";
	private final Set<UUID> vanished = new HashSet<>();

	public Vanish(Main instance) {
		super(instance, "Misc.Vanish", "vanish");
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		return Collections.emptyList();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player p = playerOrNull(sender);
		if (p == null) {
			return true;
		}
		if (noPermission(sender, cmd)) {
			return true;
		}

		if (vanished.contains(p.getUniqueId())) {
			unvanish(p);
		} else {
			vanish(p);
		}
		return true;
	}

	private void vanish(Player p) {
		vanished.add(p.getUniqueId());
		p.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, true));

		for (Player online : Bukkit.getOnlinePlayers()) {
			if (online != p && !hasPermission(online, permission)) {
				Schedulers.entity(plugin, online, () -> online.hidePlayer(plugin, p));
			}
		}

		Util.coloredMessage(p, configUtils.lang("VANISH_ENABLED"));
	}

	private void unvanish(Player p) {
		vanished.remove(p.getUniqueId());
		p.removeMetadata(METADATA_KEY, plugin);

		for (Player online : Bukkit.getOnlinePlayers()) {
			Schedulers.entity(plugin, online, () -> online.showPlayer(plugin, p));
		}

		Util.coloredMessage(p, configUtils.lang("VANISH_DISABLED"));
	}

	@EventHandler(ignoreCancelled = true)
	public void onJoin(PlayerJoinEvent e) {
		if (vanished.isEmpty()) {
			return;
		}
		Player joined = e.getPlayer();
		if (hasPermission(joined, permission)) {
			return;
		}

		for (UUID id : vanished) {
			Player vanishedPlayer = Bukkit.getPlayer(id);
			if (vanishedPlayer != null) {
				Schedulers.entity(plugin, joined, () -> joined.hidePlayer(plugin, vanishedPlayer));
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onQuit(PlayerQuitEvent e) {
		vanished.remove(e.getPlayer().getUniqueId());
	}
}
