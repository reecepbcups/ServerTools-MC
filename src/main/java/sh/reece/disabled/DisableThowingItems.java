package sh.reece.disabled;

import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;

public class DisableThowingItems implements Listener {

	private static Main plugin;
	public List<String> itemsToStopThrowing;
	private ConfigUtils configUtils;
	
	public DisableThowingItems(Main instance) {
		plugin = instance;

		if (plugin.enabledInConfig("Disabled.DisableEntityThrowing.Enabled")) {
			configUtils = plugin.getConfigUtils();
			itemsToStopThrowing = plugin.getConfig().getStringList("Disabled.DisableEntityThrowing.Items");
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
		}
	}

	
	@EventHandler(ignoreCancelled = true)
	public void stopEnder(PlayerInteractEvent e) {
		if (e.getPlayer() instanceof Player) {
			//Player p = e.getPlayer();

			// if it has an index its in the array
			if (itemsToStopThrowing.contains(e.getMaterial().toString())) {
				e.getPlayer().sendMessage(configUtils.lang("DISABLED_THROWING_ITEMS"));
				e.setCancelled(true);
			}
		
						
		} 
		
	}
	
}
