package sh.reece.disabled;

import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.block.LeavesDecayEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;

public class DisableLeaveDecay extends ToggleableListener {

	public List<String> LeaveDecayWorlds;

	public DisableLeaveDecay(Main instance) {
		super(instance, "Disabled.DisableLeaveDecay");

		if (isEnabled()) {
			this.LeaveDecayWorlds = plugin.getConfig().getStringList("Disabled.DisableLeaveDecay.WorldsToDisable");
		}
	}


	@EventHandler(ignoreCancelled = true)
	public void onDecay(LeavesDecayEvent e) {
		if(LeaveDecayWorlds.contains(e.getBlock().getWorld().getName().toString())) {
			e.setCancelled(true);
		}

	}

}
