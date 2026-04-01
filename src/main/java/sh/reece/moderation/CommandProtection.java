package sh.reece.moderation;

import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.HashMap;
import java.util.List;

public class CommandProtection implements Listener, CommandExecutor {

	private static Main plugin;
	private FileConfiguration config;
	private final String Section;
	private String FILENAME;
	private String password;

	private List<String> ProtectedCommands;
	private List<String> AllowedPlayers;
	private List<String> PasswordView; // hardcoded people only can view password

	private HashMap<String, Long> FailedPassAttempts;

	private ConfigUtils configUtils;
	

	public CommandProtection(Main instance) {
		plugin = instance;

		Section = "Moderation.CommandProtect";                
		if(plugin.enabledInConfig(Section+".Enabled")) {
			configUtils = plugin.getConfigUtils();

			FILENAME = "CommandProtect.yml";
			configUtils.createConfig(FILENAME);	
			config = configUtils.getConfigFile(FILENAME);	

			AllowedPlayers = config.getStringList("AllowedPlayers");
			PasswordView = config.getStringList("AllowedPlayers");

			ProtectedCommands = config.getStringList("ProtectedCommands");
			password = config.getString("password");
			
			FailedPassAttempts = new HashMap<String, Long>();

			plugin.getCommand("commandprotect").setExecutor(this);
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);    		
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void playerCommand(PlayerCommandPreprocessEvent e) {
		String command = e.getMessage();
		Player p = e.getPlayer();

		if(e.getMessage().length() > 1) {
			command = command.split(" ")[0];
		} 

		// Util.consoleMSG(command);

		// if the cmd is protected
		if(ProtectedCommands.contains(command)) {

			// if the IGN or UUID is allowed
			if(AllowedPlayers.contains(p.getName()) || AllowedPlayers.contains(p.getUniqueId().toString())) {
				Util.coloredMessage(p, "&7&oSTools CMDBypass - Allowing use");	
				return;
			} else {
				Util.coloredMessage(p, configUtils.lang("COMMAND_PROTECT_DENY").replace("%cmd%", command));
				e.setCancelled(true);
			}			
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		Player p = (Player) sender;
		String PName = p.getName();

		if (args.length == 0) {
			sendHelpMenu(p, label);
			return true;
		}	

		switch(args[0]){
		// /command clear
		case "login":	
			if(args.length >= 2) {
				if(password.equalsIgnoreCase(args[1])){
					Util.coloredMessage(p, "&aPassword Match!");
					if(!AllowedPlayers.contains(PName)) {
						AllowedPlayers.add(PName);
						Util.coloredMessage(p, "&aAdded too cmdbypass");
					} else {
						Util.coloredMessage(p, "&eYou are already logged in");					
					}
				} else {
					Util.coloredMessage(p, "&cPassword does not match!");

					if(FailedPassAttempts.containsKey(PName)) {
						Long times = FailedPassAttempts.get(PName);

						Util.consoleMSG(FailedPassAttempts.get(PName)+"");

						if(times >= 15) {
							p.kickPlayer("[ServerTools] Stop trying to bruteforce cmdprotect...");
							FailedPassAttempts.put(PName, (long) 1);							
						} else {
							FailedPassAttempts.put(PName, times+=1);
						}
					} else {
						FailedPassAttempts.put(PName, (long) 1);																		
					}
				} 				
			} else {
				sendHelpMenu(p, label);
			}
			return true;
		case "logout":
			if(AllowedPlayers.contains(PName)) {
				AllowedPlayers.remove(PName);
				Util.coloredMessage(p, "&cRemoved from allowing cmdbypass");
			}
			return true;

		case "password":
			if(PasswordView.contains(p.getName())) {
				Util.coloredMessage(p, "Password: " + password);
			}
			return true;

		case "loginfails": 
			if(PasswordView.contains(p.getName())) {
				Util.coloredMessage(p, FailedPassAttempts.toString());
			}
			return true;	

		default:
			sendHelpMenu(p, label);
			return true;		
		}		
	}

	public void sendHelpMenu(Player p, String cmd) {
		Util.coloredMessage(p, "&b/"+cmd+" &flogin <password>");
		Util.coloredMessage(p, "&b/"+cmd+" &flogout");

		if(PasswordView.contains(p.getName())) {
			Util.coloredMessage(p, "\n&f&oOnly hardcoded AllowedPlayers see this");
			Util.coloredMessage(p, "&b/"+cmd+" &fpassword");
			Util.coloredMessage(p, "&b/"+cmd+" &floginfails");
		}

	}



}
