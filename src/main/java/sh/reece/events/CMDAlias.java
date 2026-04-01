package sh.reece.events;

import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;
import sh.reece.utiltools.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class CMDAlias extends ToggleableListener {

	public ConfigurationSection Alises;

	private static Set<String> Disabled;
	private static HashMap<String, Set<String>> worlddisabled;

	// world: [cmd, 5]
	private static final HashMap<String, HashMap<String, Integer>> preWorldCooldown = new HashMap<String, HashMap<String, Integer>>();
	private boolean stopIfMoved = false;

	private String permission;

	private ConfigUtils configUtils;

	public CMDAlias(Main instance) {
		super(instance, "Misc.CMDAliases");

		if (isEnabled()) {
			configUtils = instance.getConfigUtils();
			Disabled = new HashSet<>(instance.getConfig().getStringList("Misc.CMDAliases.disabled"));

			permission = instance.getConfig().getString("Misc.CMDAliases.Permission");
			Alises = instance.getConfig().getConfigurationSection("Misc.CMDAliases.cmds");

			// every 15 mins it refreshes this
			Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(instance, new Runnable() {
				public void run() {
					saveDisabledCommands();
				}
			}, 0, 900 * 20L);

			if (instance.getConfig().contains("Misc.CMDAliases.preCooldownCommands")) {
				stopIfMoved = instance.getConfig().getBoolean("Misc.CMDAliases.preCooldownCommands.stopIfMoved");

				var preCooldownSection = instance.getConfig().getConfigurationSection("Misc.CMDAliases.preCooldownCommands");
				if (preCooldownSection == null) return;
				for (String world : preCooldownSection.getKeys(false)) {
					HashMap<String, Integer> tempHoldCommands = new HashMap<String, Integer>();

					if (!world.equalsIgnoreCase("stopIfMoved")) {
						for (String cmd : instance.getConfig().getStringList("Misc.CMDAliases.preCooldownCommands." + world)) {
							int pctIdx = cmd.indexOf('%');
							String command = cmd.substring(0, pctIdx);
							int timeWait = Integer.parseInt(cmd.substring(pctIdx + 1));

							tempHoldCommands.put(command.toLowerCase(), timeWait);
						}
						preWorldCooldown.put(world, tempHoldCommands);
					}
				}
			} else {
				Util.consoleMSG("&c[!] Add the following into your config (Misc.CMDAliases)");
				Util.consoleMSG("    preCooldownCommands:\r\n" +
						"      stopIfMoved: true\r\n" +
						"      warzone:\r\n" +
						"      - spawn%5\r\n" +
						"      - tpyes%5");
			}
		}
	}

	// saves all commands which should be disabled to the list.
	// Every 15 mins this is refreshed to make sure it doesnt unload
	public void saveDisabledCommands() {
		worlddisabled = new HashMap<>();
		if (plugin.getConfig().contains("Misc.CMDAliases.disabledWorlds")) {
			var disabledWorldsSection = plugin.getConfig().getConfigurationSection("Misc.CMDAliases.disabledWorlds");
			if (disabledWorldsSection == null) return;
			for (String world : disabledWorldsSection.getKeys(false)) {

				if (Bukkit.getWorld(world) != null) {
					Set<String> s = new HashSet<>();

					for (String blockCMD : plugin.getConfig().getStringList("Misc.CMDAliases.disabledWorlds." + world)) {
						s.add(blockCMD.toLowerCase());
					}
					worlddisabled.put(world, s);
				}
			}
		} else {
			Util.consoleMSG("&c[!] Add the following into your config (Misc.CMDAliases)\n    disabledWorlds:\n      WORLD:\n      - cmd");
			Util.consoleMSG("\r\n\r\n");
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onCommand(PlayerCommandPreprocessEvent e) {

		if (!(e.getMessage().length() > 1)) {
			return;
		}

		String rawMsg = e.getMessage();
		int spaceIdx = rawMsg.indexOf(' ');
		String command = (spaceIdx == -1 ? rawMsg.substring(1) : rawMsg.substring(1, spaceIdx)).toLowerCase();
		Player p = e.getPlayer();
		String world = p.getLocation().getWorld().getName();

		if (!worlddisabled.isEmpty()) {
			Set<String> blocked = worlddisabled.get(world);
			if (blocked != null) {
				if (blocked.contains(command)) {
					if (!hasPermission(e.getPlayer())) {
						e.setCancelled(true);
						Util.coloredMessage(e.getPlayer(), configUtils.lang("CMDALIAS_DENYWORLD").replace("%cmd%", command));
						return;
					} else {
						Util.coloredMessage(e.getPlayer(), "&7&oBypassing command for disable world due to perm");
					}
				}
			}
		}

		// DISABLED COMMANDS
		if (Disabled.contains(command)) {
			if (!hasPermission(e.getPlayer())) {
				e.setCancelled(true);
				Util.coloredMessage(e.getPlayer(), configUtils.lang("CMDALIAS_DISABLED").replace("%cmd%", command));
				return;
			}
		}

		HashMap<String, Integer> worldCooldowns = preWorldCooldown.get(world);
		if (worldCooldowns != null) {
			command = e.getMessage().substring(1);

			if (worldCooldowns.containsKey(command)) {

				if (hasPermission(p)) {
					Util.coloredMessage(p, "&7&oBypassing PreCommand Cooldown due to being staff");
					return;
				} else {
					e.setCancelled(true);
				}

				Location loc = p.getLocation();
				int sec = worldCooldowns.get(command);

				if (!hasPermission(p)) {
					Util.coloredMessage(p, configUtils.lang("CMDALIAS_DELAYED").replace("%cmd%", command).replace("%time%", sec + ""));
					new BukkitRunnable() {
						@Override
						public void run() {
							if (stopIfMoved) {
								if (loc.getBlockX() != p.getLocation().getBlockX() || loc.getBlockZ() != p.getLocation().getBlockZ()) {
									Util.coloredMessage(p, configUtils.lang("CMDALIAS_DELAYED_MOVED"));
									return;
								}
							}

							p.performCommand(e.getMessage().substring(1));
							return;
						}
					}.runTaskLater(plugin, sec * 20);
				}
			}
		}

		if (Alises.contains(command)) {
			String aliasResult = plugin.getConfig().getString("Misc.CMDAliases.cmds." + command);
			String userArguments = e.getMessage().substring(command.length() + 1)
					.replaceAll("%player%", e.getPlayer().getName());
			e.setMessage(e.getMessage().substring(0, 1) + aliasResult + userArguments);
		}
	}
}
