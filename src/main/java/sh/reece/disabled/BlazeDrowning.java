package sh.reece.disabled;

import org.bukkit.entity.Blaze;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;

public class BlazeDrowning extends ToggleableListener {

	public BlazeDrowning(Main instance) {
		super(instance, "Disabled.DisableBlazeDrowning");
	}


	@EventHandler(ignoreCancelled = true)
	public void onDmg(EntityDamageEvent e) {
		Entity ent = e.getEntity();
		if (ent instanceof Blaze && e.getCause() == EntityDamageEvent.DamageCause.DROWNING) {
			e.setCancelled(true);
		}
	}

}
