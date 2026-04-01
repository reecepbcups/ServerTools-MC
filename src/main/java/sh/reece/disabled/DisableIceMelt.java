package sh.reece.disabled;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockFadeEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;

public class DisableIceMelt extends ToggleableListener {

	public DisableIceMelt(Main instance) {
		super(instance, "Disabled.DisableIceMelt");
	}


	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onIceBlockMelt(BlockFadeEvent e) {
		if (e.getBlock().getType().equals(Material.ICE)){
			e.setCancelled(true);
		}
	}

}
