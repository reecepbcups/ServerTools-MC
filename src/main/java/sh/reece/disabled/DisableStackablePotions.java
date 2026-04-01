package sh.reece.disabled;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;
import sh.reece.utiltools.Util;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;

public class DisableStackablePotions extends ToggleableListener {

	public DisableStackablePotions(Main instance) {
		super(instance, "Disabled.DisableStackablePotions");
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void plashEvent(PotionSplashEvent e) {
		if(e.getPotion().getShooter() instanceof Player) {
			Player shooter = (Player) e.getPotion().getShooter();

			if(shooter.getInventory().getItemInHand().getAmount() > 1){
				Util.coloredMessage(shooter, plugin.getConfigUtils().lang("DISABLED_STACKED_POTIONS"));
				e.setCancelled(true);

				// gives player their potion back
				ItemStack newStack = shooter.getInventory().getItemInHand().clone();
				newStack.setAmount(1);
				shooter.getInventory().addItem(newStack);
			}
		}

	}
}
