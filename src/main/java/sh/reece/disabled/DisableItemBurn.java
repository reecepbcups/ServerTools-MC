package sh.reece.disabled;

import java.util.EnumSet;
import java.util.Set;

import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;

public class DisableItemBurn extends ToggleableListener {

	private Set<EntityDamageEvent.DamageCause> causes;

	public DisableItemBurn(Main instance) {
		super(instance, "Disabled.DisableItemBurn");

		if (isEnabled()) {
			causes = EnumSet.noneOf(EntityDamageEvent.DamageCause.class);
			for(String s : plugin.getConfig().getStringList("Disabled.DisableItemBurn.reasons"))
				causes.add(EntityDamageEvent.DamageCause.valueOf(s.toUpperCase()));
		}

	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onItemBurn(EntityDamageEvent e) {
		if (e.getEntity() instanceof Item && causes.contains(e.getCause())) {
			e.setCancelled(true);
		}

	}
}
