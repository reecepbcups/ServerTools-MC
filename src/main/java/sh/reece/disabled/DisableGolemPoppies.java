package sh.reece.disabled;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;

public class DisableGolemPoppies extends ToggleableListener {

	public DisableGolemPoppies(Main instance) {
		super(instance, "Disabled.DisableGolemPoppies");
	}

	// Get the 1.18 material red rose
	private Material getRedRose() {
		// TODO: idk if this works
		return Material.getMaterial("RED_ROSE");
	}

	@EventHandler(ignoreCancelled = true)
	public void removeRoses(EntityDeathEvent e) {
		if (e.getEntity().getType() == EntityType.IRON_GOLEM) {
			e.getDrops().removeIf(itemstack -> (itemstack.getType() == getRedRose()));
		}

	}


}
