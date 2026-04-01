package sh.reece.disabled;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;

public class DisableFallDamage extends ToggleableListener {

	public DisableFallDamage(Main instance) {
		super(instance, "Disabled.DisableFallDamage");
	}

	@EventHandler(ignoreCancelled = true)
	public void damageEvent(EntityDamageEvent e) {
		if (e.getCause() != DamageCause.FALL) return;
		if (e.getEntityType() != EntityType.PLAYER) return;
		Player p = (Player) e.getEntity();
		if (permission.isEmpty() || hasPermission(p)) {
			e.setCancelled(true);
		}
	}


}
