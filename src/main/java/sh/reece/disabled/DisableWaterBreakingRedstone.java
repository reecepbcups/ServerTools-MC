package sh.reece.disabled;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;

import sh.reece.tools.Main;

public class DisableWaterBreakingRedstone implements Listener {
	
	
	private List<String> items = new ArrayList<String>(); 
	
	private Main plugin;
	public DisableWaterBreakingRedstone(Main instance) {
		plugin = instance;
		
		if (plugin.enabledInConfig("Disabled.DisableWaterBreakingRedstone.Enabled")) {
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
			
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
