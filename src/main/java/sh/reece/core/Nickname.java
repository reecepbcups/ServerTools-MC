package sh.reece.core;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Schedulers;
import sh.reece.utiltools.Util;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class Nickname extends BaseCommand implements Listener {

	private static final String FILE = "nicknames.yml";

	// read from the async chat thread and PAPI (TAB asks per player per refresh), so every form
	// is colored once when the nick is set instead of on each read
	private record Nick(String raw, String colored, String prefixed, String stripped) {
		static Nick of(String raw) {
			String colored = Util.color(raw);
			return new Nick(raw, colored, PREFIX + colored, ChatColor.stripColor(colored));
		}
	}

	private static final Map<UUID, Nick> nicks = new ConcurrentHashMap<>();
	// PREFIX is stored colored
	private static String PREFIX = "", BypassPrefixPerm = "";

	private final File file;

	public Nickname(Main instance) {
		super(instance, "Core.Nickname", "nick", "nickname");
		this.file = new File(plugin.getDataFolder(), FILE);
		nicks.clear();
		if (isEnabled()) {
			PREFIX = Util.color(plugin.getConfig().getString(section + ".prefix", ""));
			BypassPrefixPerm = plugin.getConfig().getString(section + ".prefixBypass", "");
			load();
			// re-apply for anyone already online after a reload
			for (Player online : Bukkit.getOnlinePlayers()) {
				apply(online);
			}
		}
	}

	/** The name a player shows as in chat: prefix + nickname, or their real name when they have none. */
	public static String displayName(Player player) {
		Nick nick = nicks.get(player.getUniqueId());
		if (nick == null) {
			return player.getName();
		}
		return player.hasPermission(BypassPrefixPerm) ? nick.colored() : nick.prefixed();
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onChat(AsyncPlayerChatEvent e) {
		if (nicks.containsKey(e.getPlayer().getUniqueId())) {
			// a % in a nickname would break the chat format string
			e.setFormat(e.getFormat().replace("%1$s", displayName(e.getPlayer()).replace("%", "%%")));
		}
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		apply(e.getPlayer());
	}

	private static void apply(Player p) {
		Nick nick = nicks.get(p.getUniqueId());
		String name = nick == null ? p.getName() : nick.colored();
		p.setDisplayName(name);
		p.setPlayerListName(name);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (noPermission(sender, cmd)) return true;

		Player p = playerOrNull(sender);
		if (p == null) return true;

		if (args.length == 0) {
			sender.sendMessage(Util.color("&fUsage: &c/" + label + " <nickname/off>"));
			return true;
		}

		String message;
		if (args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("reset")) {
			nicks.remove(p.getUniqueId());
			message = "&a[+] Your nickname has been reset back to normal";

		} else {
			// length limits count what players see, not the & codes
			Nick nick = Nick.of(args[0]);
			String visible = nick.stripped();

			if (visible.length() < 3) {
				Util.coloredMessage(p, "&c[!] Your nickname is too short");
				return true;
			} else if (visible.length() > 20) {
				Util.coloredMessage(p, "&c[!] Your nickname is too long");
				return true;
			} else if (isNameTaken(visible, p.getUniqueId())) {
				Util.coloredMessage(p, "&c[!] Your nickname is already in use");
				return true;
			}

			nicks.put(p.getUniqueId(), nick);
			message = "&a[+] Your nickname has been changed to &f" + args[0];
		}

		apply(p);
		Schedulers.async(plugin, this::save);
		Util.coloredMessage(p, message);
		return true;
	}

	private static boolean isNameTaken(String visible, UUID self) {
		for (Player online : Bukkit.getOnlinePlayers()) {
			if (!online.getUniqueId().equals(self) && online.getName().equalsIgnoreCase(visible)) {
				return true;
			}
		}
		for (Map.Entry<UUID, Nick> entry : nicks.entrySet()) {
			if (!entry.getKey().equals(self) && entry.getValue().stripped().equalsIgnoreCase(visible)) {
				return true;
			}
		}
		return false;
	}

	private void load() {
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
		for (String key : yaml.getKeys(false)) {
			try {
				nicks.put(UUID.fromString(key), Nick.of(yaml.getString(key)));
			} catch (IllegalArgumentException e) {
				Util.consoleMSG("&cSkipping bad player id " + key + " in " + FILE);
			}
		}
	}

	// synchronized so two quick changes can't write the file out of order
	private synchronized void save() {
		YamlConfiguration yaml = new YamlConfiguration();
		nicks.forEach((id, nick) -> yaml.set(id.toString(), nick.raw()));
		try {
			yaml.save(file);
		} catch (IOException e) {
			Util.consoleMSG("&cFailed to save " + FILE + ": " + e.getMessage());
		}
	}
}
