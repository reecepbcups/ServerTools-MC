package sh.reece.disabled;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class DisableDragonEggTP extends ToggleableListener {

	public DisableDragonEggTP(Main instance) {
		super(instance, "Disabled.DisableDragonEggTP");
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
	public void onBlockInteract(PlayerInteractEvent event) {
		Block clickedBlock = event.getClickedBlock();

		if (clickedBlock == null) {
			return;
		}

		if(clickedBlock.getType() == Material.DRAGON_EGG && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			event.getPlayer().sendMessage(plugin.getConfigUtils().lang("DISABLED_DRAGON_EGG_TP"));
			event.setCancelled(true);
		}


		if(clickedBlock.getType() == Material.DRAGON_EGG && event.getAction() == Action.LEFT_CLICK_BLOCK) {

			if(!(event.getPlayer().isOp())) {
				event.setCancelled(true);
				return;
			}

			Block block = event.getClickedBlock();
			block.setType(Material.AIR);

			event.getPlayer().getInventory().addItem(new ItemStack(Material.DRAGON_EGG, 1));
			event.getPlayer().sendMessage("Removed the dragon egg due to you being an OP");
		}


	}
}
