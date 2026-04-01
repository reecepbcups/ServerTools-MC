package sh.reece.disabled;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockFromToEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;

public class DisableWaterBreakingRedstone extends ToggleableListener {

	private Set<Material> blockedMaterials;

	public DisableWaterBreakingRedstone(Main instance) {
		super(instance, "Disabled.DisableWaterBreakingRedstone");

		if (isEnabled()) {
			blockedMaterials = new HashSet<>();
			for(String s : plugin.getConfig().getStringList("Disabled.DisableWaterBreakingRedstone.items")) {
				try {
					blockedMaterials.add(Material.valueOf(s.toUpperCase()));
				} catch (IllegalArgumentException ignored) {}
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onWaterFlow(BlockFromToEvent e) {
		if (blockedMaterials.contains(e.getToBlock().getType()))
			e.setCancelled(true);
	}



}
