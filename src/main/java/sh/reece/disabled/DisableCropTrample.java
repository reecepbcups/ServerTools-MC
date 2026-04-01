package sh.reece.disabled;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;

public class DisableCropTrample extends ToggleableListener {

	public DisableCropTrample(Main instance) {
		super(instance, "Disabled.DisableCropTrample");
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteractEvent(PlayerInteractEvent e) {
		if (e.getAction() == Action.PHYSICAL && e.getClickedBlock().getType().equals(Material.FARMLAND))
			e.setCancelled(true);
	}

}
