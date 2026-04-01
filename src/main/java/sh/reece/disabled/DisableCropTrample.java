package sh.reece.disabled;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import sh.reece.tools.Main;

public class DisableCropTrample implements Listener {
	
	private static Main plugin;
	public DisableCropTrample(Main instance) {
        plugin = instance;        

        if (plugin.enabledInConfig("Disabled.DisableCropTrample.Enabled")) {
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);			
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteractEvent(PlayerInteractEvent e) {
		if (e.getAction() == Action.PHYSICAL && e.getClickedBlock().getType().equals(Material.FARMLAND))
			e.setCancelled(true);
	}
	
}
