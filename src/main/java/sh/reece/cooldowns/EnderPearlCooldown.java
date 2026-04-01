package sh.reece.cooldowns;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;
import sh.reece.utiltools.Util;

import java.util.HashMap;

public class EnderPearlCooldown extends ToggleableListener {

	private final Integer cooldownSeconds;
	private final String cooldownMessage;
	private final HashMap<String, Long> cooldownHash;

	public EnderPearlCooldown(final Main instance) {
		super(instance, "Cooldowns.EnderPearlCooldown");

		if (isEnabled()) {
			cooldownSeconds = instance.getConfig().getInt("Cooldowns.EnderPearlCooldown.Seconds");
			cooldownMessage = instance.getConfig().getString("Cooldowns.EnderPearlCooldown.Message");
			cooldownHash = new HashMap<>();
		} else {
			cooldownSeconds = 0;
			cooldownMessage = null;
			cooldownHash = null;
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onInteract(final PlayerInteractEvent e) {
		final Player p = e.getPlayer();
		final Action a = e.getAction();

		if (a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK) {
			if (p.getItemInHand().getType() == Material.ENDER_PEARL) {

				if (!(Util.cooldown(cooldownHash, cooldownSeconds, p.getName(), cooldownMessage))) {
					e.setCancelled(true);
				}
			}
		}
	}
}
