package sh.reece.disabled;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.CreatureSpawnEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;

public class DisableJockeys extends ToggleableListener {

	public DisableJockeys(Main instance) {
		super(instance, "Disabled.DisableJockeySpawning");
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
