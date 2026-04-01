package sh.reece.disabled;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

import sh.reece.tools.Main;

public class DisableHunger implements Listener {

	private static Main plugin;
	private String HungerPerm;

	public DisableHunger(Main instance) {
		plugin = instance;

		if (plugin.enabledInConfig("Disabled.DisableHunger.Enabled")) {
			this.HungerPerm = plugin.getConfig().getString("Disabled.DisableHunger.Permission");
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void foodChangeEvent(FoodLevelChangeEvent event) {
		if (event.getEntityType() == EntityType.PLAYER) {
			Player player = (Player) event.getEntity();
			if (HungerPerm.length() == 0 || player.hasPermission(HungerPerm)) {
				if (player.getFoodLevel() < 19.0D)
					player.setFoodLevel(20); 
			} 
		} 
	}

}
