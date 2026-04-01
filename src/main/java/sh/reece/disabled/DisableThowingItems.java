package sh.reece.disabled;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;

public class DisableThowingItems extends ToggleableListener {

	public List<String> itemsToStopThrowing;

	public DisableThowingItems(Main instance) {
		super(instance, "Disabled.DisableEntityThrowing");

		if (isEnabled()) {
			itemsToStopThrowing = plugin.getConfig().getStringList("Disabled.DisableEntityThrowing.Items");
		}
	}


	@EventHandler(ignoreCancelled = true)
	public void stopEnder(PlayerInteractEvent e) {
		if (e.getPlayer() instanceof Player) {

			// if it has an index its in the array
			if (itemsToStopThrowing.contains(e.getMaterial().toString())) {
				e.getPlayer().sendMessage(plugin.getConfigUtils().lang("DISABLED_THROWING_ITEMS"));
				e.setCancelled(true);
			}


		}

	}

}
