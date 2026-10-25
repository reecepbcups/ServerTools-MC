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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
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
	private static String PREFIX = "", BypassPrefixPerm = "", ColorPerm = "";

	// raw input cap, so a 20 character nick can't carry hundreds of color codes into chat and tab
	private static final int MAX_RAW_LENGTH = 64;
	private static final Pattern VISIBLE_NAME = Pattern.compile("[A-Za-z0-9_]+");

	private final File file;

	public Nickname(Main instance) {
		super(instance, "Core.Nickname", "nick", "nickname");
		this.file = new File(plugin.getDataFolder(), FILE);
		nicks.clear();
		if (isEnabled()) {
			PREFIX = Util.color(plugin.getConfig().getString(section + ".prefix", ""));
			BypassPrefixPerm = plugin.getConfig().getString(section + ".prefixBypass", "");
			ColorPerm = plugin.getConfig().getString(section + ".colorPerm", "nickname.color");
			String check = plugin.getConfig().getString(section + ".NameCheck", "joined");
			try {
				nameCheck = NameCheck.valueOf(check.trim().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				Util.consoleMSG("&c[Nickname] Unknown NameCheck '" + check + "', using joined");
				nameCheck = NameCheck.JOINED;
			}
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

		if (args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("clear")) {
			nicks.remove(p.getUniqueId());
			finish(p, "&a[+] Your nickname has been reset back to normal");
			return true;
		}

		// length limits count what players see, not the & codes
		Nick nick = Nick.of(args[0]);
		String visible = nick.stripped();

		// coloring changed nothing means there were no codes or gradients in it
		if (!nick.colored().equals(args[0]) && !p.hasPermission(ColorPerm)) {
			Util.coloredMessage(p, "&c[!] You can't use colors in your nickname");
			return true;
		} else if (args[0].length() > MAX_RAW_LENGTH) {
			Util.coloredMessage(p, "&c[!] Your nickname has too many color codes");
			return true;
		} else if (!VISIBLE_NAME.matcher(visible).matches()) {
			// same characters as real usernames, so look-alike letters can't dodge the name check
			Util.coloredMessage(p, "&c[!] Nicknames can only use letters, numbers and _");
			return true;
		} else if (visible.length() < 3) {
			Util.coloredMessage(p, "&c[!] Your nickname is too short");
			return true;
		} else if (visible.length() > 20) {
			Util.coloredMessage(p, "&c[!] Your nickname is too long");
			return true;
		} else if (isNameTaken(visible, p)) {
			Util.coloredMessage(p, "&c[!] Your nickname is already in use");
			return true;
		}

		if (nameCheck != NameCheck.REAL || visible.equalsIgnoreCase(p.getName())) {
			setNick(p, nick);
			return true;
		}

		// account lookup is network IO, so it runs off thread and the result comes back to the player
		realAccountExists(visible).whenComplete((exists, error) -> Schedulers.entity(plugin, p, () -> {
			if (error != null) {
				Util.coloredMessage(p, "&c[!] Couldn't check that name right now, try again in a bit");
			} else if (exists) {
				Util.coloredMessage(p, "&c[!] That's a real player's name, pick another");
			} else {
				setNick(p, nick);
			}
		}));
		return true;
	}

	private void setNick(Player p, Nick nick) {
		nicks.put(p.getUniqueId(), nick);
		finish(p, "&a[+] Your nickname has been changed to &f" + nick.raw());
	}

	private void finish(Player p, String message) {
		apply(p);
		Schedulers.async(plugin, this::save);
		Util.coloredMessage(p, message);
	}

	/** Which player names a nick may not copy. Other players' nicks are always off limits. */
	enum NameCheck {
		ANY,    // nothing extra
		JOINED, // anyone who has played on this server
		REAL    // JOINED, plus any real Minecraft account (looked up with Mojang)
	}

	private static NameCheck nameCheck = NameCheck.JOINED;

	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
	// names don't stop existing, so each one is looked up once. Lower cased.
	private static final Map<String, Boolean> REAL_NAMES = new ConcurrentHashMap<>();

	private static CompletableFuture<Boolean> realAccountExists(String name) {
		String key = name.toLowerCase(Locale.ROOT);
		Boolean cached = REAL_NAMES.get(key);
		if (cached != null) {
			return CompletableFuture.completedFuture(cached);
		}

		// name is already limited to [A-Za-z0-9_], safe in the path
		HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.mojang.com/users/profiles/minecraft/" + key))
				.timeout(Duration.ofSeconds(5)).GET().build();
		return HTTP.sendAsync(request, HttpResponse.BodyHandlers.discarding()).thenApply(response -> {
			int status = response.statusCode();
			if (status == 200 || status == 204 || status == 404) {
				boolean exists = status == 200;
				REAL_NAMES.put(key, exists);
				return exists;
			}
			// rate limited or Mojang is down: don't guess, and don't cache
			throw new IllegalStateException("account lookup returned " + status);
		});
	}

	private static boolean isNameTaken(String visible, Player self) {
		if (nameCheck != NameCheck.ANY) {
			// anyone who has ever joined, online or not, so staff can't be impersonated while offline
			OfflinePlayer known = Bukkit.getOfflinePlayerIfCached(visible);
			if (known != null && !known.getUniqueId().equals(self.getUniqueId())) {
				return true;
			}
			for (Player online : Bukkit.getOnlinePlayers()) {
				if (online != self && online.getName().equalsIgnoreCase(visible)) {
					return true;
				}
			}
		}
		for (Map.Entry<UUID, Nick> entry : nicks.entrySet()) {
			if (!entry.getKey().equals(self.getUniqueId()) && entry.getValue().stripped().equalsIgnoreCase(visible)) {
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
