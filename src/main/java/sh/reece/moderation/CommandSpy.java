package sh.reece.moderation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class CommandSpy implements Listener, CommandExecutor {

	private static Main plugin;
	private FileConfiguration config;
	private String Section, Permission;
	private List<String> ignored;

	private static ArrayList<UUID> watching;
	

	public CommandSpy(Main instance) {
		plugin = instance;

		Section = "Moderation.CommandSpy";                
		if(plugin.enabledInConfig(Section+".Enabled")) {

			config = plugin.getConfig();
			Permission = config.getString(Section+".permission");
			ignored = config.getStringList(Section+".Ignored-ignored_commands");
			
			plugin.getCommand("commandspy").setExecutor(this);
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);    
			watching = new ArrayList<>();
		}
	}

	public static boolean isWatching(UUID uuid) {
		// used in PAPI
		return watching.contains(uuid);
	}

	@EventHandler(ignoreCancelled = true)
	public void playerCommandSpyEvent(PlayerCommandPreprocessEvent e) {	
		if (e.getPlayer().hasPermission("commandspy.exempt")) {
			return;
		}

		String m = e.getMessage().toLowerCase();
		
		if (ignored.contains(m.split(" ")[0]) || ignored.contains(m)) {
			return; 
		}
			
		String n = e.getPlayer().getName();
		String msg = e.getMessage();
		Bukkit.getServer().getOnlinePlayers().forEach(x -> {
			if (watching.contains(x.getUniqueId()) && !x.getUniqueId().equals(e.getPlayer().getUniqueId())) {
				x.sendMessage(Util.color("&7CMDSPY &f&n"+n+ "&8> &f " + msg)); 
			}
				
		});			
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {		
		if (!(sender.hasPermission(Permission))) {		
			sender.sendMessage(Util.color("&cNo Permission to use "+label+" :("));
			return true;			
		}

		Player p = (Player) sender;

		if (args.length == 0) {
			sendHelpMenu(p);
			return true;
		}	

		switch(args[0].toLowerCase()){
		// /command clear
		case "enable":
		case "e":
		case "start":
			if(watching.contains(p.getUniqueId())) {
				p.sendMessage("You already have this on");
			} else {
				Util.coloredMessage(p, "&7Commandspy has been &aenabled&7.");
				watching.add(p.getUniqueId());
			}
			return true;
			
		case "disable":
		case "d":
		case "stop":
			Util.coloredMessage(p, "&7Commandspy has been &cdisabled&7.");
			
			if(watching.contains(p.getUniqueId())) {
				watching.remove(p.getUniqueId());
			}			
			return true;
		default:
			sendHelpMenu(p);
			return true;		
		}		
	}

	public void sendHelpMenu(Player p) {
		Util.coloredMessage(p, "&f/commandspy &7enable/disable");
	}
}
