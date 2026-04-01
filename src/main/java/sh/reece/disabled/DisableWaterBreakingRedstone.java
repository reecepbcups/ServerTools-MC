package sh.reece.disabled;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockFromToEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;

public class DisableWaterBreakingRedstone extends ToggleableListener {


	private List<String> items = new ArrayList<String>();

	public DisableWaterBreakingRedstone(Main instance) {
		super(instance, "Disabled.DisableWaterBreakingRedstone");

		if (isEnabled()) {
			for(String s : plugin.getConfig().getStringList("Disabled.DisableWaterBreakingRedstone.items")) {
				items.add(s.toUpperCase());
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onWaterFlow(BlockFromToEvent e) {
		if (items.contains(e.getToBlock().getType().toString()))
			e.setCancelled(true);
	}



}
