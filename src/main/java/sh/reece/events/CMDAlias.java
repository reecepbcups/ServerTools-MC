package sh.reece.events;

import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CMDAlias implements Listener {

	public ConfigurationSection Alises;

	private static List<String> Disabled;
	private static HashMap<String, List<String>> worlddisabled;

	// world: [cmd, 5]
	private static final HashMap<String, HashMap<String, Integer>> preWorldCooldown = new HashMap<String, HashMap<String,Integer>>();
	private boolean stopIfMoved = false;
	
	
	//public FileConfiguration config;
	private String aliasResult, userArguments, permission;

	public Main plugin;
	private ConfigUtils configUtils;
	public CMDAlias(Main instance) {
		plugin = instance;


		if (plugin.enabledInConfig("Misc.CMDAliases.Enabled")) {
			configUtils = plugin.getConfigUtils();
			Disabled = plugin.getConfig().getStringList("Misc.CMDAliases.disabled");

			//config = configUtils.getConfigFile("config.yml");
			permission = plugin.getConfig().getString("Misc.CMDAliases.Permission");			 		
			Alises = plugin.getConfig().getConfigurationSection("Misc.CMDAliases.cmds");
			//Disabled = plugin.getConfig().getStringList("Misc.CMDAliases.disabled");

			// every 15 mins it refreshes this
			Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
				public void run() {
					saveDisabledCommands();					
				}
			}, 0, 900*20L); // 15 minutes with initial delay of 0 seconds (run now)
			

			// on command run, get player location, wait X seconds, if they have not moved allowed command to be run.
			if(plugin.getConfig().contains("Misc.CMDAliases.preCooldownCommands")) {
				stopIfMoved = plugin.getConfig().getBoolean("Misc.CMDAliases.preCooldownCommands.stopIfMoved");
				//Util.consoleMSG("&aloaded preCooldownCommands stopifMoved");
				
				for(String world : plugin.getConfig().getConfigurationSection("Misc.CMDAliases.preCooldownCommands").getKeys(false)) {
					HashMap<String, Integer> tempHoldCommands = new HashMap<String, Integer>();
					
					if(!world.equalsIgnoreCase("stopIfMoved")) {								
						for(String cmd : plugin.getConfig().getStringList("Misc.CMDAliases.preCooldownCommands."+world)) {
							String command = cmd.split("%")[0];
							Integer timeWait = Integer.valueOf(cmd.split("%")[1]);
							
							//Util.consoleMSG("&eloaded "+command+" for " + timeWait + " seconds for "+world);							
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
			
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);

		}
	}

	// saves all commands which should be disabled to the list.
	// Every 15 mins this is refreshed to make sure it doesnt unload
	public void saveDisabledCommands() {
		// Util.consoleMSG("&aServerTools - Refreshed DisabledCommands to hash");
		// new init here so it clears previous
		worlddisabled = new HashMap<String, List<String>>();
		if(plugin.getConfig().contains("Misc.CMDAliases.disabledWorlds")) {
			// gets worlds to disable specific cmds (Grabs exact copy bc sometimes is weird with this)
			for(String world : plugin.getConfig().getConfigurationSection("Misc.CMDAliases.disabledWorlds").getKeys(false)) {

				if(Bukkit.getWorld(world) != null) {
					//Util.consoleMSG("World " + world + " Found for CMDAlias Disable");
					List<String> l = new ArrayList<String>();

					// if world is real, block all commands
					for(String blockCMD : plugin.getConfig().getStringList("Misc.CMDAliases.disabledWorlds."+world)) {
						l.add(blockCMD.toLowerCase());
					}
					worlddisabled.put(world, l);
				} else {
					// Util.consoleMSG("&cWORLD: " + world + " in CMDDisabler is not valid!");
				}
			}
		} else {
			Util.consoleMSG("&c[!] Add the following into your config (Misc.CMDAliases)\n    disabledWorlds:\n      WORLD:\n      - cmd");
			Util.consoleMSG("\r\n" + 
					"\r\n" + 
					"");
		}
	}
	

	String command;
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onCommand(PlayerCommandPreprocessEvent e) {

		if (!(e.getMessage().length() > 1)) {
			return;
		}

		command = e.getMessage().substring(1).split(" ")[0].toLowerCase();
		Player p = e.getPlayer();
		String world = p.getLocation().getWorld().getName();
		
		
		// if there are any keys
		if(worlddisabled.keySet().size() > 0) {
			// if player in world which there is a key for
			if(worlddisabled.keySet().contains(world)) {
				// if the cmd they ran is in the list disabled for that world
				if(worlddisabled.get(world).contains(command)) {
					// cancel if no bypass perm
					if(!e.getPlayer().hasPermission(permission)) {
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
		if(Disabled.contains(command)) {
			if(!e.getPlayer().hasPermission(permission)) {
				e.setCancelled(true);
				Util.coloredMessage(e.getPlayer(), configUtils.lang("CMDALIAS_DISABLED").replace("%cmd%", command));
				return;
			}
		}

		
		//Util.consoleMSG(preWorldCooldown.keySet().toString());
		if(preWorldCooldown.containsKey(world)) {
			//Util.consoleMSG("&e"+p.getName()+" is in the world!");
			
			// update command WITH spaces
			command = e.getMessage().substring(1); // removes /
			//Util.consoleMSG(command);
			
			// if that world has a command which is suppose to be on preCooldown
			//Util.consoleMSG(preWorldCooldown.get(world).toString());
			if(preWorldCooldown.get(world).keySet().contains(command)) {
				
				if(p.hasPermission(permission)) {
					Util.coloredMessage(p, "&7&oBypassing PreCommand Cooldown due to being staff");
					return;
				} else {
					e.setCancelled(true);
				}
				
				
				//Util.consoleMSG(p.getName()+" is in world " + world);
				
				Location loc = p.getLocation();
				int sec = preWorldCooldown.get(world).get(command);
				
				if(!p.hasPermission(permission)) {
					Util.coloredMessage(p, configUtils.lang("CMDALIAS_DELAYED").replace("%cmd%", command).replace("%time%", sec+""));
					new BukkitRunnable() {					
						@Override
						public void run() {
							if(stopIfMoved) {
								if(loc.getBlockX() != p.getLocation().getBlockX() || loc.getBlockZ() != p.getLocation().getBlockZ()) {								
									Util.coloredMessage(p, configUtils.lang("CMDALIAS_DELAYED_MOVED"));
									return;
								} 
							} 

							// returns original command they wanted to run
							p.performCommand(e.getMessage().substring(1));
							return;
						}
					}.runTaskLater(plugin, sec*20); // should get the INT value from the hashmap	
				}
			}			
		}
		

		if(Alises.contains(command)) {
			aliasResult = plugin.getConfig().getString("Misc.CMDAliases.cmds."+command);

			userArguments = e.getMessage().substring(command.length() + 1)
					.replaceAll("%player%", e.getPlayer().getName());

			e.setMessage(e.getMessage().substring(0, 1) + aliasResult + userArguments);
		} 		

	}

}
