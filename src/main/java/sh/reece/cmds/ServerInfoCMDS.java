package sh.reece.cmds;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import me.clip.placeholderapi.PlaceholderAPI;
import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class ServerInfoCMDS implements Listener {//, CommandExecutor {

	private static Main plugin;
	private FileConfiguration config;
	private String Section;
	private Set<String> commands;
	// command -> its message lines, only for commands with Enabled=true. Parsed once
	// at load so the hot path is a single map lookup instead of a fresh YAML read.
	private Map<String, List<String>> enabledMessages;

	public ServerInfoCMDS(Main instance) {
        plugin = instance;

        Section = "ServerInfoCMDS";

        if(plugin.getConfigUtils().enabledInConfig(Section+".Enabled")) {

        	config = ConfigUtils.getInstance().createConfig("ServerInfoCommands.yml");

        	// ex. [discord, buy]
        	commands = config.getKeys(false);

        	enabledMessages = new HashMap<>();
        	for(String cmd : commands) {
        		if("true".equalsIgnoreCase(config.getString(cmd+".Enabled"))) {
        			enabledMessages.put(cmd, config.getStringList(cmd+".message"));
        		}
        	}

			if(commands.size() > 0) {
				Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
			}
    	}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        String lowerMSG = e.getMessage().toLowerCase();

        if(!lowerMSG.startsWith("/")) {
        	return;
        }

        int space = lowerMSG.indexOf(' ');
        String cmd = (space == -1 ? lowerMSG.substring(1) : lowerMSG.substring(1, space));
        if(commands.contains(cmd)) {

        	List<String> lines = enabledMessages.get(cmd);
        	if(lines != null) {
        		for(String s : lines){

        			if(plugin.isPAPIEnabled()) {
						s = PlaceholderAPI.setPlaceholders(p, s);
					}

            		Util.coloredMessage(p, ConfigUtils.replaceVariable(s));
            	}
        	}
        	e.setCancelled(true);
        }
    }
}
