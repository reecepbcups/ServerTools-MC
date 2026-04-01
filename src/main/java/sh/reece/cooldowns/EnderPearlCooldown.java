package sh.reece.cooldowns;

import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Date;
import java.util.HashMap;

public class EnderPearlCooldown implements Listener {

	private final Main plugin;
	private final Integer cooldownSeconds;
	private final String cooldownMessage;
	private final HashMap<String, Date> cooldownHash;

	public EnderPearlCooldown(final Main instance) {
        plugin = instance;

        cooldownSeconds = plugin.getConfig().getInt("Cooldowns.EnderPearlCooldown.Seconds");
        cooldownHash = new HashMap<>();

        cooldownMessage = plugin.getConfig().getString("Cooldowns.EnderPearlCooldown.Message");

        if (plugin.enabledInConfig("Cooldowns.EnderPearlCooldown.Enabled")) {
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
		}
	}



	@EventHandler(ignoreCancelled = true)
    public void onInteract(final PlayerInteractEvent e) {
        final Player p = e.getPlayer();
        final Action a = e.getAction();

        if (a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK) {
            if (p.getItemInHand().getType() == Material.ENDER_PEARL) {

            	if (!(Util.cooldown(cooldownHash, cooldownSeconds, p.getName(), cooldownMessage))) {
            		e.setCancelled(true);
            	}
            }
        }
    }
}
