package sh.reece.disabled;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;

import sh.reece.tools.Main;

public class DisableMobSpawning implements Listener {
    private static Main plugin;
	private FileConfiguration MAINCONFIG;
	private String Section;
	private Set<String> worlds;

	public DisableMobSpawning(Main instance) {
		plugin = instance;
		Section = "Disabled.DisableMobSpawning";

		if(plugin.enabledInConfig(Section+".Enabled")) {

			MAINCONFIG = plugin.getConfig();
			worlds = new HashSet<>(MAINCONFIG.getStringList(Section+".worldsToDisable"));

			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);

		}
	}

	@EventHandler(ignoreCancelled = true)
	public void NoMobSpawning(EntitySpawnEvent e) {
        if (e.getEntity() instanceof Creature || e.getEntity() instanceof Monster) {
            if (worlds.isEmpty() || worlds.contains(e.getEntity().getWorld().getName())) {
                e.setCancelled(true);
            }
        }
	}
}
