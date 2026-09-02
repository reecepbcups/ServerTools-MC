package sh.reece.runnables;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

import sh.reece.tools.Main;
import sh.reece.utiltools.Schedulers;
import sh.reece.utiltools.Util;

public class ClearLag implements CommandExecutor {

	private static Main plugin;
	private FileConfiguration config;
	private String Section, ClearSoonMSG, ClearedMSG;
	private int delay;
	Set<Integer> warningTimes = new HashSet<>(Arrays.asList(5, 10, 30, 60, 120));
	private Boolean firstRun;
	private Boolean AutoClearMobs;

	public ClearLag(Main instance) {
		plugin = instance;

		Section = "Misc.ClearLag";
		if (plugin.getConfigUtils().enabledInConfig(Section + ".Enabled")) {

			config = plugin.getConfig();

			Boolean AutoClearItems = config.getString(Section + ".AutoClearItems.Enabled").equalsIgnoreCase("true");

			AutoClearMobs = config.getString(Section + ".AutoClearItems.ClearMobs").equalsIgnoreCase("true");

			delay = config.getInt(Section + ".AutoClearItems.ClearDelay");
			firstRun = true;

			if (AutoClearItems) {
				Schedulers.global(plugin, this::run);
			}

			ClearSoonMSG = config.getString(Section + ".AutoClearItems.ClearSoonMSG");
			ClearedMSG = config.getString(Section + ".AutoClearItems.ClearedMSG");

			plugin.getCommand("clearlag").setExecutor(this);
		}
	}

	private int test;

	public void run() {
		test = delay;

		Schedulers.globalTimer(plugin, () -> {

			if (test <= 0) {
				clearItemsInAllWorlds();
				test = delay;
				return;
			}

			if (warningTimes.contains(test)) {
				Util.coloredBroadcast(ClearSoonMSG.replace("%seconds%", test + ""));
			}

			test -= 5;
		}, 0, 5 * 20L);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender.hasPermission("tools.clearlag"))) {
			sender.sendMessage(Util.color("&cNo Permission to use " + label + " :("));
			return true;
		}

		Player p = (Player) sender;

		if (args.length == 0) {
			sendHelpMenu(p);
			return true;
		}

		switch (args[0]) {
			case "clear":
				clearItemsInAllWorlds();
				Util.coloredMessage(p, "&aYou cleared all items on the ground");
				return true;

			case "radius":
			case "rad":
			case "r":
				if (args.length < 2) {
					sendHelpMenu(p);
					return true;
				}

				int radius = Integer.parseInt(args[1]);

				for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
					if (e instanceof Item) {
						e.remove();
					}
				}

				return true;

			default:
				sendHelpMenu(p);
				return true;
		}
	}

	public void clearItemsInAllWorlds() {

		if (firstRun) {
			firstRun = !firstRun;
			return;
		}

		if (Schedulers.isFolia()) {
			// Folia: no thread may touch a whole world at once. Sweep each loaded chunk on
			// the region thread that owns it.
			clearItemsFolia();
		} else {
			// Paper: spread entity removal across ticks to avoid stalling the main thread.
			// each world gets its own deferred task so we don't process thousands of entities in one tick.
			List<World> worlds = Bukkit.getWorlds();
			for (int i = 0; i < worlds.size(); i++) {
				final World w = worlds.get(i);
				final boolean clearMobs = AutoClearMobs;
				Schedulers.globalLater(plugin, () -> {
					for (Entity e : w.getEntities()) {
						removeIfClearable(e, clearMobs);
					}
				}, i); // stagger by 1 tick per world
			}
		}
		Util.coloredBroadcast(ClearedMSG);
	}

	// per-chunk region sweep - the Folia-safe way to clear a world's entities.
	private void clearItemsFolia() {
		final boolean clearMobs = AutoClearMobs;
		for (World w : Bukkit.getWorlds()) {
			for (Chunk c : w.getLoadedChunks()) {
				Schedulers.regionChunk(plugin, w, c.getX(), c.getZ(), () -> {
					for (Entity e : c.getEntities()) {
						removeIfClearable(e, clearMobs);
					}
				});
			}
		}
	}

	private void removeIfClearable(Entity e, boolean clearMobs) {
		if (e instanceof Item) {
			e.remove();
		} else if (clearMobs && (e instanceof Animals || e instanceof Monster) && e.getCustomName() == null) {
			e.remove();
		}
	}

	public void sendHelpMenu(Player p) {
		Util.coloredMessage(p, "&f/clearlag &7clear");
		Util.coloredMessage(p, "&f/clearlag &7radius <blocks>");
	}
}
