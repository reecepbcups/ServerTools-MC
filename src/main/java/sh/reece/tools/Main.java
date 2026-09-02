package sh.reece.tools;

import java.io.File;
import java.util.Collections;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import sh.reece.core.economy.EconomyStorage;
import sh.reece.core.economy.EcoFormat;
import sh.reece.core.economy.ServerToolsEconomy;
import sh.reece.utiltools.Util;

public class Main extends JavaPlugin implements Listener {

	public final String PREFIX = "&f&lSERVER &8\u00bb&r ";

	private static boolean isPAPIEnabled;

	public static Chat chat = null; // used for Tags
	private Loader loader;
	private ConfigUtils configUtils;

	private EconomyStorage economyStorage;
	private String currencySymbol = "$";

	public void onLoad() {
		// Register the Vault economy provider during LOAD, before any plugin's onEnable.
		// Economy consumers (EconomyShopGUI, etc.) look up the provider in their own
		// onEnable; if we waited for our onEnable - which runs after theirs by alphabetical
		// order - they'd find nothing and disable themselves. onLoad runs for every plugin
		// before any onEnable, so registering here guarantees we're ready in time.
		setupEconomy();
	}

	public void onEnable() {
		loader = new Loader(this);
		configUtils = new ConfigUtils(this);

		configUtils.loadConfig();
		loader.setMarking("Configurations");

		// economy is already registered in onLoad; record it in the module list for /tools
		configUtils.enabledInConfig("Economy.Enabled");

		loader.loadAll();

		Collections.sort(configUtils.modulesList);
		loader.output();

		// warm the material sets now so the first isArmour/isWeapon/isTool call
		// during play doesn't pay for the full material scan mid-tick
		Util.initMaterialSets();

		// Must be last - sets up fallback aliases for disabled commands
		new AlternateCommandHandler(this);
	}

	public void onDisable() {
		loader.unloadAll();
		if (economyStorage != null) {
			economyStorage.close();
		}
	}

	/**
	 * Stand up the economy provider: open the SQLite store, then register it with
	 * Vault. Runs in onLoad, so it reads config directly (configUtils isn't built yet)
	 * and skips silently if the module is off or Vault isn't installed - other modules
	 * already degrade gracefully when no economy is present.
	 */
	private void setupEconomy() {
		saveDefaultConfig(); // ensure config.yml exists before we read it in onLoad
		if (!getConfig().getString("Economy.Enabled", "true").equalsIgnoreCase("true")) {
			return;
		}
		if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
			Util.consoleMSG("&cEconomy enabled but Vault is not installed - skipping.");
			return;
		}

		EcoFormat.clearCache(); // fresh config -> drop any cached message templates
		currencySymbol = getConfig().getString("Economy.CurrencySymbol", "$");
		String singular = getConfig().getString("Economy.CurrencyNameSingular", "Dollar");
		String plural = getConfig().getString("Economy.CurrencyNamePlural", "Dollars");
		double startingDollars = getConfig().getDouble("Economy.StartingBalance", 0.0);
		long startingCents = Math.round(startingDollars * 100.0);

		try {
			Class.forName("org.sqlite.JDBC"); // ensure the driver is registered
		} catch (ClassNotFoundException ignored) {
			// modern JDBC auto-registers via ServiceLoader; ignore if missing here
		}

		File dataDir = new File(getDataFolder(), "data");
		dataDir.mkdirs();
		File db = new File(dataDir, "economy.db");
		economyStorage = new EconomyStorage("jdbc:sqlite:" + db.getAbsolutePath(), startingCents);
		try {
			economyStorage.open();
		} catch (Exception e) {
			Util.consoleMSG("&cFailed to open economy database: " + e.getMessage());
			economyStorage = null;
			return;
		}

		ServerToolsEconomy economy = new ServerToolsEconomy(economyStorage, currencySymbol, singular, plural);
		Bukkit.getServicesManager().register(Economy.class, economy, this, ServicePriority.Highest);
		Util.log("&aEconomy provider registered with Vault.");
	}

	public EconomyStorage getEconomyStorage() {
		return economyStorage;
	}

	public String getCurrencySymbol() {
		return currencySymbol;
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

	// jar file name (e.g. "servertools-8.2.0.jar"), used so Plugman can load by jar.
	//
	// We can't use getFile().getName(): Paper's runtime remapper hands back the
	// remapped copy under .paper-remapped/ (e.g. "servertools-8.2.0-<timestamp>.jar"),
	// which isn't in the plugins/ folder, so `plugman load` NPEs and leaves us
	// unloaded. Resolve the actual jar sitting in plugins/ instead.
	public String getJarFileName() {
		File pluginsDir = getDataFolder().getParentFile();
		String version = getDescription().getVersion();
		File[] jars = pluginsDir.listFiles((dir, name) -> {
			String n = name.toLowerCase();
			return n.endsWith(".jar") && n.startsWith("servertools");
		});
		if (jars != null && jars.length > 0) {
			for (File f : jars) {
				if (f.getName().contains(version)) {
					return f.getName();
				}
			}
			return jars[0].getName();
		}
		return getFile().getName(); // fallback
	}
}
