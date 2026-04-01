package sh.reece.events;


import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import org.bukkit.Bukkit;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class NoBedExplosion implements Listener {

	private static Main plugin;
	private final String Section;
	private ConfigUtils configUtils;

	public NoBedExplosion(Main instance) {
		plugin = instance;

		Section = "Events.NoBedExplosionInNether";                
		if(plugin.enabledInConfig(Section+".Enabled")) {
			configUtils = plugin.getConfigUtils();
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);    		
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onInteract(PlayerInteractEvent e){
		Player p = e.getPlayer();
		if(e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if(e.getClickedBlock().toString().toLowerCase().contains("bed")) {
				if(e.getClickedBlock().getLocation().getWorld().getEnvironment() == Environment.NETHER) {
					Util.coloredMessage(p, configUtils.lang("NOBEDEXPLOSION"));
					e.setCancelled(true);
				}
			}
		}

	}
}
