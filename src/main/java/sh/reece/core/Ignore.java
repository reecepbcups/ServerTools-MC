package sh.reece.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

// /ignore <player> toggles, /ignore lists. Hides their chat and drops their /msg.
// Saved on the player (PDC) so there's no file to write.
public class Ignore extends BaseCommand implements Listener {

	private static final NamespacedKey KEY = new NamespacedKey("servertools", "ignoring");

	// online players only, filled on join so the async chat thread never reads PDC
	private static final Map<UUID, Set<UUID>> ignoring = new ConcurrentHashMap<>();
	private static String exemptPerm = "";
	private static volatile boolean active;

	public Ignore(Main instance) {
		super(instance, "Core.Ignore", "ignore");
		ignoring.clear();
		active = isEnabled();
		if (active) {
			exemptPerm = plugin.getConfig().getString(section + ".ExemptPerm", "essentials.ignoreexempt");
			for (Player online : Bukkit.getOnlinePlayers()) {
				load(online);
			}
		}
	}

	/** True when receiver has ignored sender. Staff with the exempt permission can't be ignored. */
	public static boolean isIgnoring(Player receiver, Player sender) {
		if (!active) return false;
		Set<UUID> set = ignoring.get(receiver.getUniqueId());
		return set != null && set.contains(sender.getUniqueId()) && !sender.hasPermission(exemptPerm);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onChat(AsyncPlayerChatEvent e) {
		if (ignoring.isEmpty()) return;
		Player sender = e.getPlayer();
		try {
			e.getRecipients().removeIf(receiver -> isIgnoring(receiver, sender));
		} catch (UnsupportedOperationException ignored) {
			// another plugin made the recipient set read only, nothing we can do
		}
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		load(e.getPlayer());
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		ignoring.remove(e.getPlayer().getUniqueId());
	}

	private static void load(Player p) {
		List<String> stored = p.getPersistentDataContainer().get(KEY, PersistentDataType.LIST.strings());
		if (stored == null || stored.isEmpty()) return;

		Set<UUID> set = ConcurrentHashMap.newKeySet();
		for (String id : stored) {
			try {
				set.add(UUID.fromString(id));
			} catch (IllegalArgumentException ignored) {
			}
		}
		ignoring.put(p.getUniqueId(), set);
	}

	private static void save(Player p, Set<UUID> set) {
		List<String> ids = new ArrayList<>(set.size());
		for (UUID id : set) ids.add(id.toString());
		if (ids.isEmpty()) {
			p.getPersistentDataContainer().remove(KEY);
		} else {
			p.getPersistentDataContainer().set(KEY, PersistentDataType.LIST.strings(), ids);
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (noPermission(sender, cmd)) return true;
		Player p = playerOrNull(sender);
		if (p == null) return true;

		Set<UUID> set = ignoring.computeIfAbsent(p.getUniqueId(), k -> ConcurrentHashMap.newKeySet());

		if (args.length == 0) {
			if (set.isEmpty()) {
				Util.coloredMessage(p, "&fYou aren't ignoring anyone. &7/" + label + " <player>");
				return true;
			}
			List<String> names = new ArrayList<>(set.size());
			for (UUID id : set) {
				String name = Bukkit.getOfflinePlayer(id).getName();
				names.add(name == null ? id.toString() : name);
			}
			Util.coloredMessage(p, "&fIgnoring&7: &e" + String.join("&7, &e", names));
			return true;
		}

		OfflinePlayer target = Bukkit.getPlayerExact(args[0]);
		if (target == null) {
			target = Bukkit.getOfflinePlayerIfCached(args[0]);
		}
		if (target == null) {
			Util.coloredMessage(p, "&c" + args[0] + " &fhas never joined.");
			return true;
		}
		if (target.getUniqueId().equals(p.getUniqueId())) {
			Util.coloredMessage(p, "&cYou can't ignore yourself.");
			return true;
		}

		if (set.remove(target.getUniqueId())) {
			Util.coloredMessage(p, "&aYou are no longer ignoring &f" + target.getName() + "&a.");
		} else {
			if (target.getPlayer() != null && target.getPlayer().hasPermission(exemptPerm)) {
				Util.coloredMessage(p, "&cYou can't ignore &f" + target.getName() + "&c.");
				return true;
			}
			set.add(target.getUniqueId());
			Util.coloredMessage(p, "&aYou are now ignoring &f" + target.getName() + "&a.");
		}
		save(p, set);
		return true;
	}
}
