package sh.reece.disabled;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPhysicsEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;

public class DisableGrassDecay extends ToggleableListener {

	public DisableGrassDecay(Main instance) {
		super(instance, "Disabled.DisableGrassDecay");
	}



	@EventHandler(ignoreCancelled = true)
	public void onDecay(BlockPhysicsEvent e) {

		if(e.getBlock().getType() == Material.GRASS || e.getBlock().getType() == Material.TALL_GRASS){
			e.setCancelled(true);
		}

	}


}
