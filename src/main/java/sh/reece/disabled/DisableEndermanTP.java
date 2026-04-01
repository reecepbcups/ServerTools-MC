package sh.reece.disabled;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityTeleportEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;

public class DisableEndermanTP extends ToggleableListener {

	public DisableEndermanTP(Main instance) {
		super(instance, "Disabled.DisableEndermanTP");
	}


	@EventHandler(ignoreCancelled = true)
	public void DisableEmanTP(EntityTeleportEvent e) {
		if(e.getEntity().getType() == EntityType.ENDERMAN) {
			e.setCancelled(true);
		}
	}

}
