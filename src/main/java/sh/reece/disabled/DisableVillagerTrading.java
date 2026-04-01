package sh.reece.disabled;

import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;
import sh.reece.utiltools.Util;

public class DisableVillagerTrading extends ToggleableListener {

	private String Message;

	public DisableVillagerTrading(Main instance) {
		super(instance, "Disabled.DisableVillagerTrading");

		if (isEnabled()) {
			Message = plugin.getConfig().getString("Disabled.DisableVillagerTrading.Message");
			Message = Message.replace("%perm%", permission);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void villagerTrade(InventoryOpenEvent event) {
		if (event.getInventory().getType() != InventoryType.MERCHANT) {
			return;
	    }

		if(!hasPermission(event.getPlayer())) {
			Util.coloredMessage(event.getPlayer(), Message);
			event.setCancelled(true);
		}


		return;
	  }

}
