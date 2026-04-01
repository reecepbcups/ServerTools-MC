package sh.reece.disabled;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;

import sh.reece.tools.Main;

public class DisableDisconnectSpam implements Listener {
	
	private Main plugin;
	public DisableDisconnectSpam(Main instance) {
	    this.plugin = instance;
	    
	    if (plugin.enabledInConfig("Disabled.DisableDisconnectSpamKick.Enabled")) {
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);			
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onKick(PlayerKickEvent e) {
		if (e.getReason() == "disconnect.spam") {
			e.setCancelled(true);
		}
	}

}
