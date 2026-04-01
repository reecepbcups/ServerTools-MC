package sh.reece.cooldowns;

import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Date;
import java.util.HashMap;

public class GoldenAppleCooldown implements Listener {

	private final Main plugin;
	private final Integer cooldownSeconds;
	private final String cooldownMessage;
	private String eatenMessage;
	private final HashMap<String, Date> appleCooldown;
	
	public GoldenAppleCooldown(final Main instance) {
        plugin = instance;

		cooldownSeconds = plugin.getConfig().getInt("Cooldowns.GoldenAppleCooldown.Seconds");
		appleCooldown = new HashMap<>();

		cooldownMessage = plugin.getConfig().getString("Cooldowns.GoldenAppleCooldown.Message");
		eatenMessage = plugin.getConfig().getString("Cooldowns.GoldenAppleCooldown.StartCooldownMSG");
        eatenMessage = eatenMessage.replace("%seconds%", cooldownSeconds.toString());
        
        if (plugin.enabledInConfig("Cooldowns.GoldenAppleCooldown.Enabled")) {
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);			
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void gappleCooldown(final PlayerItemConsumeEvent e) {
		final Player p = e.getPlayer();
		final ItemStack TYPE = e.getItem();
		final ItemStack apple = new ItemStack(Material.GOLDEN_APPLE, 1, (short)0);
		
		if (TYPE.getType().equals(Material.GOLDEN_APPLE) && TYPE.getData().equals(apple.getData())) {
			if (!(Util.cooldown(appleCooldown, cooldownSeconds, p.getName(), cooldownMessage))) {
        		e.setCancelled(true);        	
        	} else {
        		if(eatenMessage.length() > 0) {
        			Util.coloredMessage(p, eatenMessage);
        		}
        		
        	}
		} 
	}
	
	
}
