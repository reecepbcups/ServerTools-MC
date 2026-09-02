package sh.reece.disabled;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;

public class DisableMobAI extends ToggleableListener {

	private Set<String> worlds;

	public DisableMobAI(Main instance) {
		super(instance, "Disabled.DisableMobAI");

		if (isEnabled()) {
			worlds = new HashSet<>(plugin.getConfig().getStringList("Disabled.DisableMobAI.worldsToDisable"));
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void MobAI(EntityTargetLivingEntityEvent e) {
		if (worlds.isEmpty()) {
			return;
		}
		if (worlds.contains(e.getEntity().getWorld().getName())) {
			e.setCancelled(true);
		}
	}

}
