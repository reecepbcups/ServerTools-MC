package sh.reece.tools;

import java.util.Collections;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.chat.Chat;

public class Main extends JavaPlugin implements Listener {

	public final String PREFIX = "&f&lSERVER &8\u00bb&r ";

	private static boolean isPAPIEnabled;

	public static Chat chat = null; // used for Tags
	private Loader loader;
	private ConfigUtils configUtils;

	public void onEnable() {
		loader = new Loader(this);
		configUtils = new ConfigUtils(this);

		configUtils.loadConfig();
		loader.setMarking("Configurations");

		loader.loadAll();

		Collections.sort(configUtils.modulesList);
		loader.output();

		// Must be last - sets up fallback aliases for disabled commands
		new AlternateCommandHandler(this);
	}

	public void onDisable() {
		loader.unloadAll();
	}

	public static boolean isPAPIEnabled() {
		return isPAPIEnabled;
	}

	public void setPAPIStatus(boolean state) {
		isPAPIEnabled = state;
	}

	public ConfigUtils getConfigUtils() {
		return configUtils;
	}
}
