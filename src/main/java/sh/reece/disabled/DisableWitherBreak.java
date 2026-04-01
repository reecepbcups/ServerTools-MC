package sh.reece.disabled;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;

public class DisableWitherBreak extends ToggleableListener {

	public DisableWitherBreak(Main instance) {
		super(instance, "Disabled.DisableWitherBlockBreak");
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
