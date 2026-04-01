package sh.reece.disabled;

import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class DisableCaneOnCane implements Listener {

	private static Main plugin;
	private ConfigUtils configUtils;
	public DisableCaneOnCane(Main instance) {
        plugin = instance;
        
        if (plugin.enabledInConfig("Disabled.DisableCaneTowers.Enabled")) {
			configUtils = plugin.getConfigUtils();
    		Bukkit.getServer().getPluginManager().registerEvents(this, plugin);			
    	}
	}
	
	
	
	
	@EventHandler(ignoreCancelled = true)
	public void onBookWrite(BlockPlaceEvent e) {
		Player p = e.getPlayer();
		Block b = e.getBlock().getLocation().add(0,-1,0).getBlock();
		
		//e.getPlayer().sendMessage(b.getType() + " : " + e.getPlayer().getItemInHand().getType().toString());	

		if(p.getInventory().getItemInMainHand().getType() == Material.SUGAR_CANE) {
			if(b.getType() == Material.SUGAR_CANE) {
				p.sendMessage(configUtils.lang("DISABLED_CANE_ON_CANE"));
				e.setCancelled(true);
			}
			
		}
		
		
		
		
	}
	
	
}
