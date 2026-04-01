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

public class GodAppleCooldown implements Listener {

	private final Main plugin;
	private final Integer cooldownSeconds;
	private final String cooldownMessage;
	private String eatenMessage;
	private final HashMap<String, Date> appleCooldown;
	private static final ItemStack GOD_APPLE = new ItemStack(Material.GOLDEN_APPLE, 1, (short)1);

	public GodAppleCooldown(final Main instance) {
        plugin = instance;

		cooldownSeconds = plugin.getConfig().getInt("Cooldowns.GodAppleCooldown.Seconds");
		appleCooldown = new HashMap<>();

		cooldownMessage = plugin.getConfig().getString("Cooldowns.GodAppleCooldown.Message");
		eatenMessage = plugin.getConfig().getString("Cooldowns.GodAppleCooldown.StartCooldownMSG");
        eatenMessage = eatenMessage.replace("%seconds%", cooldownSeconds.toString());

        if (plugin.enabledInConfig("Cooldowns.GodAppleCooldown.Enabled")) {
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void gappleCooldown(final PlayerItemConsumeEvent e) {
		final Player p = e.getPlayer();
		final ItemStack TYPE = e.getItem();

		if (TYPE.getType().equals(Material.GOLDEN_APPLE) && TYPE.getData().equals(GOD_APPLE.getData())) {
			if(!(Util.cooldown(appleCooldown, cooldownSeconds, p.getName(), cooldownMessage))) {
        		e.setCancelled(true);
        	} else {
        		if(eatenMessage.length() > 0) {
        			Util.coloredMessage(p, eatenMessage);
        		}

        	}
		}
	}


}
