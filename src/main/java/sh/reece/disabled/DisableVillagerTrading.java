package sh.reece.disabled;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;

import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class DisableVillagerTrading implements Listener {

	private static Main plugin;
	private String section = "Disabled.DisableVillagerTrading";
	private String permission, Message;
	public DisableVillagerTrading(Main instance) {
	        plugin = instance;
	        
	        if (plugin.enabledInConfig(section+".Enabled")) {
				Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
				permission = plugin.getConfig().getString(section+".Permission");
				Message = plugin.getConfig().getString(section+".Message");
				Message = Message.replace("%perm%", permission);
			}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void villagerTrade(InventoryOpenEvent event) {
		if (event.getInventory().getType() != InventoryType.MERCHANT) {
			return; 
	    }
	    
		if(!event.getPlayer().hasPermission(permission)) {
			Util.coloredMessage(event.getPlayer(), Message);
			event.setCancelled(true);
		}
		
		
		return;
	  }
	
}
