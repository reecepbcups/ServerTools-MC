package sh.reece.disabled;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerKickEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;

public class DisableDisconnectSpam extends ToggleableListener {

	public DisableDisconnectSpam(Main instance) {
		super(instance, "Disabled.DisableDisconnectSpamKick");
	}

	@EventHandler(ignoreCancelled = true)
	public void onKick(PlayerKickEvent e) {
		if (e.getReason() == "disconnect.spam") {
			e.setCancelled(true);
		}
	}

}
