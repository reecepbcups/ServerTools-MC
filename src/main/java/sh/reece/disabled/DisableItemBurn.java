package sh.reece.disabled;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;

public class DisableItemBurn extends ToggleableListener {

	private List<EntityDamageEvent.DamageCause> causes = new ArrayList<>();

	public DisableItemBurn(Main instance) {
		super(instance, "Disabled.DisableItemBurn");

		if (isEnabled()) {
			for(String s : plugin.getConfig().getStringList("Disabled.DisableItemBurn.reasons"))
				causes.add(EntityDamageEvent.DamageCause.valueOf(s.toUpperCase()));
		}

	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onItemBurn(EntityDamageEvent e) {
		if (!e.isCancelled() && e.getEntity() instanceof Item && causes.contains(e.getCause())) {
			e.setCancelled(true);
		}

	}
}
