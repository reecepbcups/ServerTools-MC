package sh.reece.disabled;


import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTeleportEvent;

import sh.reece.tools.Main;

public class DisableEndermanTP implements Listener {//, CommandExecutor {

	private static Main plugin;
	//private FileConfiguration MAINCONFIG;
	private String Section;

	public DisableEndermanTP(Main instance) {
		plugin = instance;

		Section = "Disabled.DisableEndermanTP";        

		if(plugin.enabledInConfig(Section+".Enabled")) {

			//MAINCONFIG = plugin.getConfig();

			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);

		}
	}

	
	@EventHandler(ignoreCancelled = true)
	public void DisableEmanTP(EntityTeleportEvent e) {	
		if(e.getEntity().getType() == EntityType.ENDERMAN) {
			e.setCancelled(true);
		}	
	}
	
}
