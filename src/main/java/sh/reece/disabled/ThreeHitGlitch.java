package sh.reece.disabled;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;

public class ThreeHitGlitch extends ToggleableListener {

	public ThreeHitGlitch(Main instance) {
		super(instance, "Misc.ThreeHitGlitch");
	}

	@EventHandler(ignoreCancelled = true)
	public void hurt(EntityDamageByEntityEvent event) {
		if (event.getDamager() instanceof Player) {
			Player player = (Player)event.getDamager();
			if (player.getInventory().getItemInHand().getType() == Material.AIR)
				event.setDamage(1.0D);
		}
	}



}
