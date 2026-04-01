package sh.reece.disabled;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.FoodLevelChangeEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;

public class DisableHunger extends ToggleableListener {

	public DisableHunger(Main instance) {
		super(instance, "Disabled.DisableHunger");
	}

	@EventHandler(ignoreCancelled = true)
	public void foodChangeEvent(FoodLevelChangeEvent event) {
		if (event.getEntityType() == EntityType.PLAYER) {
			Player player = (Player) event.getEntity();
			if (permission.isEmpty() || hasPermission(player)) {
				if (player.getFoodLevel() < 19.0D)
					player.setFoodLevel(20);
			}
		}
	}

}
