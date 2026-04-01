package sh.reece.disabled;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

import sh.reece.tools.Main;

public class DisableMobAI implements Listener {//, CommandExecutor {

	private static Main plugin;
	private FileConfiguration MAINCONFIG;
	private String Section;
	private List<String> worlds;

	public DisableMobAI(Main instance) {
		plugin = instance;        
		Section = "Disabled.DisableMobAI";        

		if(plugin.enabledInConfig(Section+".Enabled")) {

			MAINCONFIG = plugin.getConfig();               	
			worlds = MAINCONFIG.getStringList(Section+".worldsToDisable");

			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);

		}
	}

	@EventHandler(ignoreCancelled = true)
	public void MobAI(EntityTargetLivingEntityEvent e) {
		if (worlds.contains(e.getEntity().getLocation().getWorld().getName())) {
			e.setCancelled(true); 
		}
	}
	
}
