package sh.reece.tools;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import sh.reece.utiltools.ConfigUpdater;
import sh.reece.utiltools.Metrics;
import sh.reece.utiltools.Util;

public class ConfigUtils {

	private Main plugin;
	private static ConfigUtils configInstance;

	private final HashMap<String, String> LANG = new HashMap<>();

	private static final String VERSION_FILE = "VERSION.yml";
	private static final String BACKUP_FOLDER = "Backups";
	private static final DateFormat dateFormat = new SimpleDateFormat("MMMM-dd-yyyy_HH:mm:ss-a");

	// moved from Main
	public static final Map<String, String> SERVER_VARIABLES = new HashMap<>();
	public static final List<String> SERVER_VARIABLE_KEYS = new ArrayList<>();
	public static final List<String> ENV_VARIABLE_PATHS = new ArrayList<>();
	public final List<String> modulesList = new ArrayList<>();

	public ConfigUtils(Main instance) {
		plugin = instance;
		configInstance = this;
	}

	public static ConfigUtils getInstance() {
		// used for backups
		return configInstance;
	}

	public String getBackupDir() {
		return BACKUP_FOLDER;
	}

	public void loadConfig() {
		// check if file exist
		// if yes, get version key from file & check against current version
		// if version is different, zip plugin.getDataFolder() & save in the
		// plugin.getDataFolder() /Backup directory
		// if it does not exist, create file & save current version

		// check if "VERSION.yml" exists in plugin.getDataDirectory()

		FileConfiguration versionConfig = createConfig(VERSION_FILE);

		// December-20-2021_10:14:53-AM
		
		String ver = plugin.getDescription().getVersion();

		String verString = versionConfig.getString("version");
		if (verString != null) {
			if (!verString.equalsIgnoreCase(ver)) {				
				Util.log("Versions do not match " + verString + "->" + ver + ". Creating backup");
				createBackup(null);
			}
		} 

		// // update version to new version every reload
		versionConfig.set("version", ver);
		saveConfig(versionConfig, VERSION_FILE);

		// BStats Metrics
		new Metrics(plugin, 11289);

		createConfig("config.yml");
		plugin.getConfig().options().copyDefaults(true);

		reloadLanguage(plugin.getConfig().getString("Language"));

		try {
			ConfigUpdater.update(plugin, "config.yml", new File(plugin.getDataFolder(), "config.yml"),
					new ArrayList<String>());
		} catch (final IOException e) {
			e.printStackTrace();
		}

		_loadLocalServerVariableKeys();
	}

	
	/**
	 * @param FileName
	 * @param ignoreFolders
	 * @return String[filename, success_status]
	 */
	public String[] createBackup(String FileName, String[] ignoreFolders) {
		if(FileName == null || FileName.length() == 0){
			FileName = dateFormat.format(new Date());
		}		

		String STOOLS_DIR = plugin.getDataFolder().getAbsolutePath();

		String BACKUP_DIR = createDirectory(BACKUP_FOLDER).getAbsolutePath();
		String BACKUP_PATH = BACKUP_DIR + File.separator + FileName + ".zip";
		Util.log(STOOLS_DIR + "->\n" + BACKUP_PATH);		

		boolean success_value = Util.zipFolder(STOOLS_DIR, BACKUP_PATH, ignoreFolders);
							
		// Util.log("Backup complete");
		return new String[] {FileName + ".zip", Boolean.toString(success_value)};
	}

	public String[] createBackup(String FileName) {
		return createBackup(FileName, new String[] {BACKUP_FOLDER});
	}

	public String[] createBackup() {
		return createBackup(null, new String[] {BACKUP_FOLDER});
	}

	public String restoreBackup(String FileName){
		String BACKUP_DIR = createDirectory(BACKUP_FOLDER).getAbsolutePath();
		String BACKUP_PATH = BACKUP_DIR + File.separator + FileName + ".zip";
		Util.log("restoreBackup " + BACKUP_PATH);
				
		File backupFile = new File(BACKUP_PATH);
		if(!backupFile.exists()){
			Util.log("Backup file does not exist");
			return "Backup file does not exist";
		}

		Util.unzipFile(BACKUP_PATH, plugin.getDataFolder().getAbsolutePath());
		Util.log("Restore complete");	
		return "&e[!] Restore Complete! Reloading configs...";
	}
	

	private void _loadLocalServerVariableKeys() {
		for (String key : plugin.getConfig().getConfigurationSection("PluginVariables").getKeys(false)) {
			SERVER_VARIABLES.put(key, plugin.getConfig().getString("PluginVariables." + key));
			SERVER_VARIABLE_KEYS.add(key);
		}
	}

	public FileConfiguration getConfigFile(final String name) {
		return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), name));
	}

	public File createDirectory(final String DirName) {
		final File newDir = new File(plugin.getDataFolder(), DirName.replace("/", File.separator));
		if (!newDir.exists()) {
			newDir.mkdirs();
		}
		return newDir;
	}

	public FileConfiguration createConfig(final String name) {
		final File file = new File(plugin.getDataFolder(), name);

		if (!new File(plugin.getDataFolder(), name).exists()) {
			plugin.saveResource(name, false);
		}

		final FileConfiguration configuration = getConfigFile(name);
		if (!file.exists()) {
			try {
				configuration.save(file);
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		return configuration;
	}

	public void createFile(final String name) {
		final File file = new File(plugin.getDataFolder(), name);

		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void saveConfig(final FileConfiguration config, final String name) {
		try {
			config.save(new File(plugin.getDataFolder(), name));
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public void reloadLanguage(String lang) {
		LANG.clear();

		createDirectory("translations");
		createConfig("translations/" + lang + ".yml");
		final FileConfiguration language = getConfigFile("translations/" + lang + ".yml");
		for (final String key : language.getKeys(false)) {
			LANG.put(key, language.getString(key));
		}
	}

	public String lang(final String key) {
		return Util.color(LANG.get(key).replace("%prefix%", "&7[&eServerTools&7]&r"));
	}

	// enabledInConfig + env variable support (moved from Main)

	public static String getPathENVKey(String path) {
		return "SERVERTOOLS_" + path.toUpperCase(Locale.ROOT).replace('-', '_').replace('.', '_');
	}

	public static String resolveValue(String path) {
		String key = getPathENVKey(path);
		String value = System.getenv(key);
		if (value != null) {
			Util.log("[ServerTools] Found value from environment variable " + key + ": " + value);
		}
		return value;
	}

	public boolean enabledInConfig(final String path) {
		String module = "";
		try {
			if (!plugin.getConfig().contains(path)) {
				Util.consoleMSG(Util.color("&c[TOOLS] " + path + " does not exist!!!"));
				return false;
			}

			String configValue = plugin.getConfig().getString(path);
			ENV_VARIABLE_PATHS.add(path);
			if (!configValue.equalsIgnoreCase("true")) {
				configValue = "false";
			}

			String myEnvVariable = resolveValue(path);
			if (myEnvVariable != null) {
				Util.log(getPathENVKey(path) + " set too: " + myEnvVariable);
				configValue = myEnvVariable;
			}

			final String pathInfo = replaceUnNeededInfo(path);
			boolean isEnabled = configValue.equalsIgnoreCase("true");
			if (isEnabled) {
				module = "&a" + pathInfo;
			} else {
				module = "&c" + pathInfo;
			}
			modulesList.add(module + "&f,&r ");
			return isEnabled;

		} catch (final Exception e) {
			modulesList.add("&4" + module + "&f,&r ");
		}
		return false;
	}

	public static String replaceVariable(String line) {
		if (!line.contains("%")) return line;
		for (var entry : SERVER_VARIABLES.entrySet()) {
			line = line.replace("%" + entry.getKey() + "%", entry.getValue());
		}
		return line;
	}

	public static String replaceUnNeededInfo(String s) {
		final String[] replace = {".Enabled", "Disabled.Disable", "Disabled.", "Events.", "Moderation.", "Cooldowns.", "Misc.", "Chat.", "Bungee.", "Commands.", "Core."};
		for (final String key : replace) {
			s = s.replace(key, "");
		}
		return s;
	}

}
