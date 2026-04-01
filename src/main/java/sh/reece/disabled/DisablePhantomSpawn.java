package sh.reece.disabled;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import sh.reece.tools.Main;

public class DisablePhantomSpawn implements Listener {
	
	private static Main plugin;
	public DisablePhantomSpawn(Main instance) {
        plugin = instance;
        
        if (plugin.enabledInConfig("Disabled.DisablePhantomSpawn.Enabled")) {
    		Bukkit.getServer().getPluginManager().registerEvents(this, plugin);			
    	}
	}
	
	
	
	@EventHandler(ignoreCancelled = true)
	public void onDecay(CreatureSpawnEvent e) {
		// does not check to make sure server is >1.13
		// if someone enables this, thats on them. idot
//		if(e.getEntity() instanceof Phantom) {
//			
//		}
		
		// incase its not 1.13
		if(e.getEntity().getName().equalsIgnoreCase("Phantom")) {
			e.setCancelled(true);
		}
		
	}
	
	
}
