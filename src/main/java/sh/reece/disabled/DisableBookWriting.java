package sh.reece.disabled;

import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;

public class DisableBookWriting implements Listener {

	private static Main plugin;
	private ConfigUtils configUtils;
	public DisableBookWriting(Main instance) {
	        plugin = instance;
	        
	        if(plugin.enabledInConfig("Disabled.DisableBookWriting.Enabled")) {
				configUtils = plugin.getConfigUtils();
				Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
			}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onBookWrite(PlayerEditBookEvent e) {
		e.getPlayer().sendMessage(configUtils.lang("DISABLED_BOOKWRITING"));
		e.setCancelled(true);			
	}
	
	
	
}
