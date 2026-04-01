package sh.reece.disabled;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.Creature;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntitySpawnEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;

public class DisableMobSpawning extends ToggleableListener {

	private Set<String> worlds;

	public DisableMobSpawning(Main instance) {
		super(instance, "Disabled.DisableMobSpawning");

		if (isEnabled()) {
			worlds = new HashSet<>(plugin.getConfig().getStringList("Disabled.DisableMobSpawning.worldsToDisable"));
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void NoMobSpawning(EntitySpawnEvent e) {
        if (e.getEntity() instanceof Creature) {
            if (worlds.isEmpty() || worlds.contains(e.getEntity().getWorld().getName())) {
                e.setCancelled(true);
            }
        }
	}
}
