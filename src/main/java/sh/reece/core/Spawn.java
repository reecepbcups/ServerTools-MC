package sh.reece.core;

import sh.reece.tools.AlternateCommandHandler;
import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import sh.reece.utiltools.Schedulers;

import java.util.HashSet;
import java.util.Set;

public class Spawn implements Listener, CommandExecutor {

	private static Main plugin;
	private final String Section;
	private String Permission, CommandPermission;

	private static FileConfiguration Config;
	private static String spawnLoc;

	private static Set<String> voidDisabledWorlds = new HashSet<>();
	private String voidmsg, spawnOnInitJoin, spawnFirstUniqueJoinOnly;
	private boolean voidTPEnabled;
	private ConfigUtils configUtils;
	
	public Spawn(Main instance) {
		plugin = instance;

		Section="Core.Spawn"; // change to Core.
		if (plugin.getConfigUtils().enabledInConfig(Section+".Enabled")) {
			configUtils = plugin.getConfigUtils();

			configUtils.createConfig("spawn.yml");
			Config = configUtils.getConfigFile("spawn.yml");
			spawnLoc = Config.getString("spawn.location");

			Permission = plugin.getConfig().getString(Section+".Permission"); // to send others to spawn
			CommandPermission = plugin.getConfig().getString(Section+".CommandPermission");

			spawnOnInitJoin = plugin.getConfig().getString(Section+".onJoinInstantly");
			spawnFirstUniqueJoinOnly = plugin.getConfig().getString(Section+".spawnFirstUniqueJoinOnly");
			
			voidTPEnabled = plugin.getConfig().getBoolean(Section+".teleportWhenInVoid.Enabled", false);			

			voidmsg = plugin.getConfig().getString(Section+".teleportWhenInVoid.message");
			voidDisabledWorlds = new HashSet<>(plugin.getConfig().getStringList(Section+".teleportWhenInVoid.disabledWorlds"));

			plugin.getCommand("spawn").setExecutor(this);
			plugin.getCommand("setspawn").setExecutor(this);
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
		} else {
			AlternateCommandHandler.addDisableCommand("spawn");
		}
	}

	// spawn & /setspawn command
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = (Player) sender;

		if (command.getLabel().equalsIgnoreCase("setspawn")) {

			if (!player.hasPermission(Permission)) {
				Util.coloredMessage(sender, "&cYou can not use the setspawn command! :(");
				return true;
			} 
			
			Location l = player.getLocation();
			spawnLoc = l.getWorld().getName()+";"+l.getX()+";"+l.getY()+";"+l.getZ()+";"+l.getYaw()+";"+l.getPitch();

			Config.set("spawn.location", spawnLoc);
			configUtils.saveConfig(Config, "spawn.yml");
			Util.coloredMessage(sender, configUtils.lang("SPAWN_SET"));
			return true;			
		}


		if (command.getLabel().equalsIgnoreCase("spawn")) {
			
			if (!sender.hasPermission(CommandPermission)) {
				Util.coloredMessage(sender, "&cYou do not have access to &n/spawn&c.");
				return true;
			} 

			String output = "";
			if (spawnLoc == "" || spawnLoc == null) {
				output = configUtils.lang("SPAWN_NONE");
				
			} else {
				if(args.length == 0) {
					output = configUtils.lang("SPAWN_TP");
					player.teleportAsync(getSpawnLocation());

				} else {	
					
					if(sender.hasPermission(Permission)) {
						Player target = Bukkit.getPlayer(args[0]);
						
						if(target != null) {
							output = "&7&l[&c&l!&7&l] &fSent " + args[0] + " to spawn!";
							target.teleportAsync(getSpawnLocation());
							Util.coloredMessage(target, configUtils.lang("SPAWN_SENT_TO_SPAWN").replace("%sender%", sender.getName()));
						}	
					} else {
						output = "&7&l[&c&l!&7&l] &cYou dont have permission for that... &7("+Permission+")";
					}	
				}
			}
			
			Util.coloredMessage(sender, output);
			return true;
		}
		return true;
	}

	@EventHandler(ignoreCancelled = true) //(priority = EventPriority.NORMAL)
	public void onDamageFromVoid(EntityDamageEvent e) {
		// when player takes damage in void, teleport them to spawn
		// if they were hurt because of void & are a player
		if(!(e.getCause() == DamageCause.VOID)) {
			return;
		}

		if(!(e.getEntity().getLocation().getY() < -50)) {
			return;
		}

		if(e.getEntity() instanceof Player) {
			if(voidTPEnabled) {
				Player p = (Player) e.getEntity();

				// if worlds is not null and player is not in the world which we did not enable
				if (voidDisabledWorlds != null && voidDisabledWorlds.contains(p.getLocation().getWorld().getName())) {
					return;
				} 

				if(getSpawnLocation() != null) {
					Util.log("To spawn.");
					// move player to spawn
					p.teleportAsync(getSpawnLocation());
					e.setCancelled(true);
					Util.coloredMessage(p, voidmsg);
					
				} else {
					// teleport them to the default spawn
					p.teleportAsync(p.getWorld().getSpawnLocation());
					Util.coloredMessage(p, voidmsg);
				}			
			} 
		}
	}

	@EventHandler(ignoreCancelled = true) //(priority = EventPriority.HIGHEST)
	public void playerKillEvent(PlayerRespawnEvent e) {
		e.setRespawnLocation(getSpawnLocation());
	}
	
	// if player has not played before, force their spawn to be
	// in the right place
	@EventHandler(ignoreCancelled = true) //(priority = EventPriority.HIGHEST)
	public void playerFirstTime(PlayerSpawnLocationEvent e) {
		if(!e.getPlayer().hasPlayedBefore()) {
			e.setSpawnLocation(getSpawnLocation());
		}		
	}

	@EventHandler(ignoreCancelled = true) //(priority = EventPriority.HIGHEST)
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		
		if((!p.hasPlayedBefore())) {
			if(spawnFirstUniqueJoinOnly.equalsIgnoreCase("true")) {
				sendToSpawn(p);		
				return;
			}			
		}
		
		
		if(spawnOnInitJoin.equalsIgnoreCase("true")) {
			sendToSpawn(p);
		}
	}

	public void sendToSpawn(Player p) {
		Schedulers.entityLater(plugin, p, () -> {
			p.teleportAsync(getSpawnLocation());
		}, 2L);
	}
	
	public Location getSpawnLocation() {
		String[] loc = spawnLoc.split(";");
		Location location = new Location(Bukkit.getWorld(loc[0]),
				Double.parseDouble(loc[1]), 
				Double.parseDouble(loc[2])+0.5, 
				Double.parseDouble(loc[3]), 
				Float.parseFloat(loc[4]), 
				Float.parseFloat(loc[5]));
		return location;
	}
}
