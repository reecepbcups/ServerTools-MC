package sh.reece.disabled;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashSet;
import java.util.Set;

public class DisableThowingItems extends ToggleableListener {

	private Set<Material> itemsToStopThrowing;

	public DisableThowingItems(Main instance) {
		super(instance, "Disabled.DisableEntityThrowing");

		if (isEnabled()) {
			itemsToStopThrowing = new HashSet<>();
			for (String s : plugin.getConfig().getStringList("Disabled.DisableEntityThrowing.Items")) {
				try {
					itemsToStopThrowing.add(Material.valueOf(s.toUpperCase()));
				} catch (IllegalArgumentException ignored) {}
			}
		}
	}


	@EventHandler(ignoreCancelled = true)
	public void stopEnder(PlayerInteractEvent e) {
		if (itemsToStopThrowing.contains(e.getMaterial())) {
			e.getPlayer().sendMessage(plugin.getConfigUtils().lang("DISABLED_THROWING_ITEMS"));
			e.setCancelled(true);
		}
	}

}
