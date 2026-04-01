package sh.reece.tools;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.PluginCommandYamlParser;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

import sh.reece.utiltools.Util;

/*
    Initated after the plugin is loaded.
*/

public class AlternateCommandHandler implements Listener {

    // on check of config.yml enabled, if false is added here. Used to give CMD aliases to other commands
	// wehn disabled
	private static final Set<String> DISABLED_COMMANDS = new HashSet<>();
	private static final HashMap<String, String> COMMAND_ALIASES = new HashMap<String, String>(); // fly: essentials, 

    
    public AlternateCommandHandler(Main plugin) {
        getCommands();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public static void addDisableCommand(String command) {
        // Bukkit.getLogger().info("Adding disabled command: " + command);
		DISABLED_COMMANDS.add(command);
	}
	public static boolean containsDisabledCommand(String command){
		return DISABLED_COMMANDS.contains(command);
	}
	public static String getCommandAlias(String command) {
        // returns OtherPluginName:Command (Essentials:fly)
		return COMMAND_ALIASES.get(command)+":"+command;
	}

    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        // Main.logging("ServerTools AltCommandListenr event: " + event.getMessage());
        // make sure its a cmd
        if(event.getMessage().startsWith("/")) {
            // Main.logging("ServerTools AltCommandListenr event: is a command with '/'");

            // make sure it is a command in there, such as "fly"
            String msg = event.getMessage();
            int si = msg.indexOf(' ');
            String cmd = si == -1 ? msg.substring(1) : msg.substring(1, si);

            if(containsDisabledCommand(cmd)) {
                // Main.logging("ServerTools AltCommandListenr event: this command is disabled");
                PluginCommand newCMD = Bukkit.getServer().getPluginCommand(cmd);
            
                // set the message to "/pluginname:command [args]" to start
                String newCommandAlias = getCommandAlias(newCMD.getName());
                String newMSG = event.getMessage().replace(cmd, newCommandAlias);

                // Main.logging(newCommandAlias + " = " + newMSG);

                event.setMessage(newMSG);
            }            
        }
        
    }

    // Run on init
    private static void getCommands() {
		COMMAND_ALIASES.clear();

        for(Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
			String pName = plugin.getName();
			if(pName.equalsIgnoreCase("ServerTools")) { continue; }

			// loop through commands for this plugin, if ours is disabled then we need to use that alias
			for(Command cmd : PluginCommandYamlParser.parse(plugin)){
				if(DISABLED_COMMANDS.contains(cmd.getName())){
					COMMAND_ALIASES.put(cmd.getName(), pName); // command, plugin_name
                    Util.log("<CMDHandler> Using Alias: " + cmd.getName() + " from " + pName);
				}
			}
		}
    }
}
