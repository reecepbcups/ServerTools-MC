package sh.reece.disabled;


import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import sh.reece.tools.Main;

public class DisableWitherBreak implements Listener {

	private static Main plugin;
	private String Section;

	public DisableWitherBreak(Main instance) {
		plugin = instance;

		Section = "Disabled.DisableWitherBlockBreak";                
		if(plugin.enabledInConfig(Section+".Enabled")) {
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);    		
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onWitherskullExplode(EntityExplodeEvent e) {
		if (e.getEntityType() == EntityType.WITHER_SKULL && e.getEntityType() == EntityType.WITHER) {
			e.blockList().clear();
			e.setCancelled(true);
		}		
	}

	@EventHandler(ignoreCancelled = true)
	public void onWitherDestroy(EntityChangeBlockEvent event) {
		if (event.getEntityType() == EntityType.WITHER) {
			event.setCancelled(true);
		}	
	}




}
