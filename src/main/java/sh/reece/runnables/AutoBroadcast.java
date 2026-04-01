package sh.reece.runnables;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import sh.reece.tools.Announcements;
import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class AutoBroadcast extends BukkitRunnable {

	private static Main plugin;
	private FileConfiguration config;
	private String Section;
	List<String> header, footer, groups;
	private Boolean center;
	private int size, id;

	private ConfigUtils configUtils;

	private List<List<String>> messages;

	public AutoBroadcast(Main instance) {
		plugin = instance;

		Section = "AutoBroadcast";
		if (plugin.getConfigUtils().enabledInConfig(Section + ".Enabled")) {

			configUtils = plugin.getConfigUtils();

			// Creates Announcements if not exist, then get that file
			configUtils.createConfig("Announcements.yml");
			config = configUtils.getConfigFile("Announcements.yml");

			header = config.getStringList("Header");
			footer = config.getStringList("Footer");

			groups = new ArrayList<String>();
			var messagesSection = config.getConfigurationSection("Messages");
			if (messagesSection == null) {
				Util.consoleMSG("&c[AutoBroadcast] Missing 'Messages' section in Announcements.yml");
				return;
			}
			for (String announcement : messagesSection.getKeys(false)) {
				groups.add(announcement);
			}

			center = config.getString("centerall").equalsIgnoreCase("true");
			size = groups.size();

			messages = new ArrayList<List<String>>();
			for (int i = 0; i < size; i++) {
				messages.add(config.getStringList("Messages." + groups.get(i)));
			}
			runTaskTimer(plugin, 0, config.getInt("Interval") * 20);
		}
	}

	@Override
	public void run() {
		if (size == id) {
			id = 0;
		} else {
			header.forEach(line -> Announcements.broadcast(center, line));

			messages.get(id).forEach(line -> {
				if (line.equalsIgnoreCase("") || line.equalsIgnoreCase(" ")) {
					line = "&r";
				}
				Announcements.broadcast(center, line);
			});

			footer.forEach(line -> Announcements.broadcast(center, line));
			id++;
		}
	}
}
