package sh.reece.cmds;

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

	public ServerInfoCMDS(Main instance) {
        plugin = instance;

        Section = "ServerInfoCMDS";

        if(plugin.getConfigUtils().enabledInConfig(Section+".Enabled")) {

        	config = ConfigUtils.getInstance().createConfig("ServerInfoCommands.yml");

        	// ex. [discord, buy]
        	commands = config.getKeys(false);

			if(commands.size() > 0) {
				Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
			}
    	}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        String lowerMSG = e.getMessage().toLowerCase();

        String cmd = lowerMSG.split(" ")[0].replace("/", "");
        if(lowerMSG.startsWith("/") && commands.contains(cmd)) {

        	if(config.getString(cmd+".Enabled").equalsIgnoreCase("true")) {
        		for(String s : config.getStringList(cmd+".message")){

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
