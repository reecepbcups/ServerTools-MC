package sh.reece.events;

import org.bukkit.Tag;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;
import sh.reece.utiltools.Util;

public class NoBedExplosion extends ToggleableListener {

	private final ConfigUtils configUtils;

	public NoBedExplosion(Main instance) {
		super(instance, "Events.NoBedExplosionInNether");
		this.configUtils = instance.getConfigUtils();
	}

	@EventHandler(ignoreCancelled = true)
	public void onInteract(PlayerInteractEvent e) {
		// PlayerInteractEvent fires once per hand; only handle the main hand
		if (e.getHand() != EquipmentSlot.HAND) {
			return;
		}
		Player p = e.getPlayer();
		if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (Tag.BEDS.isTagged(e.getClickedBlock().getType())) {
				if (e.getClickedBlock().getLocation().getWorld().getEnvironment() == Environment.NETHER) {
					Util.coloredMessage(p, configUtils.lang("NOBEDEXPLOSION"));
					e.setCancelled(true);
				}
			}
		}
	}
}
