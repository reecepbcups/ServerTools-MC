package sh.reece.cmds;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import sh.reece.disabled.DisableJLMsg;
import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Schedulers;
import sh.reece.utiltools.Util;

// minimal vanish: hides the sender from everyone lacking `permission`, and hides
// newly joining players who lack it from anyone currently vanished. Vanish survives
// relogs (not restarts). Does not touch mob AI or item drops, those are left to a
// dedicated plugin if needed.
public class Vanish extends BaseCommand implements Listener {

	private static final String METADATA_KEY = "vanished";
	// concurrent since the action bar timer reads it off the entity threads (Folia).
	private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();
	private final boolean silentJoinQuit;
	private final boolean fakeMessages;
	// when DisableJoinLeaveMsg is on, fakes mirror its custom messages (empty = none)
	private final boolean customJoinQuit;
	private final String customJoin;
	private final String customQuit;

	public Vanish(Main instance) {
		super(instance, "Misc.Vanish", "vanish");
		silentJoinQuit = plugin.getConfig().getBoolean(section + ".SilentJoinQuit", true);
		fakeMessages = plugin.getConfig().getBoolean(section + ".FakeJoinQuitMessages", false);
		customJoinQuit = configUtils.enabledInConfig(DisableJLMsg.SECTION + ".Enabled");
		customJoin = plugin.getConfig().getString(DisableJLMsg.SECTION + ".JoinMessage", "");
		customQuit = plugin.getConfig().getString(DisableJLMsg.SECTION + ".QuitMessage", "");
		if (isEnabled() && plugin.getConfig().getBoolean(section + ".ActionBar", true)) {
			actionBar = LegacyComponentSerializer.legacySection().deserialize(configUtils.lang("VANISH_ACTIONBAR"));
			Schedulers.globalTimer(plugin, this::sendActionBars, 40L, 40L);
		} else {
			actionBar = null;
		}
	}

	// parsed once, lang only changes on reload which builds a new Vanish
	private final Component actionBar;

	private void sendActionBars() {
		if (vanished.isEmpty()) {
			return;
		}
		var bar = actionBar;
		for (UUID id : vanished) {
			Player p = Bukkit.getPlayer(id);
			if (p != null) {
				Schedulers.entity(plugin, p, () -> p.sendActionBar(bar));
			}
		}
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
		hideFromOthers(p);
		broadcastFake(p, false);
		Util.coloredMessage(p, configUtils.lang("VANISH_ENABLED"));
	}

	private void hideFromOthers(Player p) {
		p.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, true));
		for (Player online : Bukkit.getOnlinePlayers()) {
			if (online != p && !hasPermission(online, permission)) {
				Schedulers.entity(plugin, online, () -> online.hidePlayer(plugin, p));
			}
		}
	}

	private void broadcastFake(Player p, boolean join) {
		if (!fakeMessages) {
			return;
		}
		String msg = customJoinQuit
				? DisableJLMsg.format(p, join ? customJoin : customQuit)
				: configUtils.lang(join ? "VANISH_FAKE_JOIN" : "VANISH_FAKE_QUIT").replace("%player%", p.getName());
		if (msg.isEmpty()) {
			return;
		}
		for (Player online : Bukkit.getOnlinePlayers()) {
			online.sendMessage(msg);
		}
	}

	private void unvanish(Player p) {
		vanished.remove(p.getUniqueId());
		p.removeMetadata(METADATA_KEY, plugin);

		for (Player online : Bukkit.getOnlinePlayers()) {
			Schedulers.entity(plugin, online, () -> online.showPlayer(plugin, p));
		}

		broadcastFake(p, true);
		Util.coloredMessage(p, configUtils.lang("VANISH_DISABLED"));
	}

	// HIGHEST so silencing wins over DisableJLMsg's custom messages.
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onJoin(PlayerJoinEvent e) {
		if (vanished.isEmpty()) {
			return;
		}
		Player joined = e.getPlayer();
		if (vanished.contains(joined.getUniqueId())) {
			if (silentJoinQuit) {
				e.setJoinMessage(null);
			}
			hideFromOthers(joined);
			Util.coloredMessage(joined, configUtils.lang("VANISH_ENABLED"));
		}
		if (hasPermission(joined, permission)) {
			return;
		}

		for (UUID id : vanished) {
			Player vanishedPlayer = Bukkit.getPlayer(id);
			if (vanishedPlayer != null && vanishedPlayer != joined) {
				Schedulers.entity(plugin, joined, () -> joined.hidePlayer(plugin, vanishedPlayer));
			}
		}
	}

	// kept in `vanished` so the player is still hidden when they relog.
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onQuit(PlayerQuitEvent e) {
		if (silentJoinQuit && vanished.contains(e.getPlayer().getUniqueId())) {
			e.setQuitMessage(null);
		}
	}
}
