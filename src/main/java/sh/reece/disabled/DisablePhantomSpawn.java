package sh.reece.disabled;

import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.CreatureSpawnEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;

public class DisablePhantomSpawn extends ToggleableListener {

	public DisablePhantomSpawn(Main instance) {
		super(instance, "Disabled.DisablePhantomSpawn");
	}



	@EventHandler(ignoreCancelled = true)
	public void onDecay(CreatureSpawnEvent e) {
		// does not check to make sure server is >1.13
		// if someone enables this, thats on them. idot

		// incase its not 1.13
		if(e.getEntity().getName().equalsIgnoreCase("Phantom")) {
			e.setCancelled(true);
		}

	}


}
