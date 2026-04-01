package sh.reece.disabled;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.event.EventHandler;
import org.bukkit.event.block.LeavesDecayEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;

public class DisableLeaveDecay extends ToggleableListener {

	private Set<String> LeaveDecayWorlds;

	public DisableLeaveDecay(Main instance) {
		super(instance, "Disabled.DisableLeaveDecay");

		if (isEnabled()) {
			this.LeaveDecayWorlds = new HashSet<>(plugin.getConfig().getStringList("Disabled.DisableLeaveDecay.WorldsToDisable"));
		}
	}


	@EventHandler(ignoreCancelled = true)
	public void onDecay(LeavesDecayEvent e) {
		if(LeaveDecayWorlds.contains(e.getBlock().getWorld().getName())) {
			e.setCancelled(true);
		}

	}

}
