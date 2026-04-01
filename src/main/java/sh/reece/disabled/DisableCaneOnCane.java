package sh.reece.disabled;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;

public class DisableCaneOnCane extends ToggleableListener {

	public DisableCaneOnCane(Main instance) {
		super(instance, "Disabled.DisableCaneTowers");
	}




	@EventHandler(ignoreCancelled = true)
	public void onBookWrite(BlockPlaceEvent e) {
		Player p = e.getPlayer();
		Block b = e.getBlock().getLocation().add(0,-1,0).getBlock();

		if(p.getInventory().getItemInMainHand().getType() == Material.SUGAR_CANE) {
			if(b.getType() == Material.SUGAR_CANE) {
				p.sendMessage(plugin.getConfigUtils().lang("DISABLED_CANE_ON_CANE"));
				e.setCancelled(true);
			}

		}
	}


}
