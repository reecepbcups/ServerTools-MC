package sh.reece.moderation;

import sh.reece.tools.AlternateCommandHandler;
import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class ColonInCommands implements Listener{

	private String perm;
	
	private final Main plugin;
	private ConfigUtils configUtils;
	public ColonInCommands(Main instance) {
		plugin = instance;
		
		if(plugin.enabledInConfig("Moderation.NoColonInCommands.Enabled")) {
			configUtils = plugin.getConfigUtils();
    		Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    		perm = plugin.getConfig().getString("Moderation.NoColonInCommands.BypassPerm");
    	}
		
	}
	
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onCommand(PlayerCommandPreprocessEvent e) {
		
		if(e.isCancelled()) {
			return;
		}		
		
		//if (e.getMessage().split(" ")[0].contains(":")) {
		if(e.getMessage().indexOf(":") != -1) {
			if(!e.getPlayer().hasPermission(perm)) {

				// Essentials:fly -> [essentials, fly, args] -? [fly, args][0]
				String CMD = e.getMessage().split(":")[1].split(" ")[0];
				if(AlternateCommandHandler.containsDisabledCommand(CMD)){
					Main.logging("[ColonInCommands] CMD Bypass due to being main alias: ");
					return;
				}

				e.getPlayer().sendMessage(configUtils.lang("NO_COLONS_IN_COMMANDS"));
				e.setCancelled(true);			
			} 
		}
	}
}
