package sh.reece.disabled;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;

import sh.reece.tools.Main;

public class DisableGrassDecay implements Listener {
	
	private static Main plugin;
	public DisableGrassDecay(Main instance) {
        plugin = instance;
        
        if (plugin.enabledInConfig("Disabled.DisableGrassDecay.Enabled")) {
    		Bukkit.getServer().getPluginManager().registerEvents(this, plugin);			
    	}
	}
	
	
	
	@EventHandler(ignoreCancelled = true)
	public void onDecay(BlockPhysicsEvent e) {
		
		if(e.getBlock().getType() == Material.GRASS || e.getBlock().getType() == Material.TALL_GRASS){
			e.setCancelled(true);
		}
		
	}
	
	
}
