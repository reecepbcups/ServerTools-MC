package sh.reece.disabled;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;

public class DisableCactusDamage extends ToggleableListener {

	public DisableCactusDamage(Main instance) {
		super(instance, "Disabled.DisableCactusDamage");
	}

	@EventHandler(ignoreCancelled = true)
	public void onDamage(EntityDamageEvent e) {
		if(e.getEntity() instanceof Player){
			if (e.getCause() == EntityDamageEvent.DamageCause.CONTACT)
				e.setCancelled(true);
			}
		}
}
