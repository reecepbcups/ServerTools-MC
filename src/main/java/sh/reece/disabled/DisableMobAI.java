package sh.reece.disabled;

import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;

public class DisableMobAI extends ToggleableListener {

	private List<String> worlds;

	public DisableMobAI(Main instance) {
		super(instance, "Disabled.DisableMobAI");

		if (isEnabled()) {
			worlds = plugin.getConfig().getStringList("Disabled.DisableMobAI.worldsToDisable");
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void MobAI(EntityTargetLivingEntityEvent e) {
		if (worlds.contains(e.getEntity().getLocation().getWorld().getName())) {
			e.setCancelled(true);
		}
	}

}
