package sh.reece.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;

public class NoAdvancementAnnouncements extends ToggleableListener {

	public NoAdvancementAnnouncements(Main instance) {
		super(instance, "Events.NoAdvancementAnnouncements");
	}

	@EventHandler
	public void onAdvancement(PlayerAdvancementDoneEvent e) {
		if (!appliesInWorld(e.getPlayer().getWorld())) {
			return;
		}
		// null message suppresses the broadcast to chat
		e.message(null);
	}
}
