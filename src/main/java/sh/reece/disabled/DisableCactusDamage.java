package sh.reece.disabled;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import sh.reece.tools.Main;

public class DisableCactusDamage implements Listener {
	
	private static Main plugin;
	public DisableCactusDamage(Main instance) {
        plugin = instance;

        if (plugin.enabledInConfig("Disabled.DisableCactusDamage.Enabled")) {
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);			
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onDamage(EntityDamageEvent e) {		
		if(e.getEntity() instanceof Player){
			if (e.getCause() == EntityDamageEvent.DamageCause.CONTACT)
				e.setCancelled(true); 
			}
		}
}
