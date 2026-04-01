package sh.reece.disabled;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import sh.reece.tools.Main;

public class DisableJockeys implements Listener {

	private static Main plugin;
	public DisableJockeys(Main instance) {
        plugin = instance;
        
        if (plugin.enabledInConfig("Disabled.DisableJockeySpawning.Enabled")) {
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
		}
	}
	  
	  @EventHandler(ignoreCancelled = true)
	    public void onSpawn(CreatureSpawnEvent event) {
	        if (event.getEntityType().equals(EntityType.CHICKEN) || event.getEntityType().equals(EntityType.SPIDER)) {
	            if (event.getEntity().getPassenger() != null) {
	                event.setCancelled(true);
	            }
	        }
	    }
	
	
}
