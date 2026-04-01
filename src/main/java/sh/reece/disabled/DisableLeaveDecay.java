package sh.reece.disabled;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.LeavesDecayEvent;

import sh.reece.tools.Main;

public class DisableLeaveDecay implements Listener {

	private static Main plugin;
	 public List<String> LeaveDecayWorlds;
	 
	public DisableLeaveDecay(Main instance) {
	        plugin = instance;
	        
	        if (plugin.enabledInConfig("Disabled.DisableLeaveDecay.Enabled")) {
	        	this.LeaveDecayWorlds = plugin.getConfig().getStringList("Disabled.DisableLeaveDecay.WorldsToDisable");	        	
				Bukkit.getServer().getPluginManager().registerEvents(this, plugin);			
			}
	}
	
	
	@EventHandler(ignoreCancelled = true)
	public void onDecay(LeavesDecayEvent e) {
		if(LeaveDecayWorlds.contains(e.getBlock().getWorld().getName().toString())) {
			e.setCancelled(true);
		}
		
	}
	
}
