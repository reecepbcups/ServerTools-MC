package sh.reece.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;

public class RemoveResourcePacksOnJoin extends ToggleableListener {

	public RemoveResourcePacksOnJoin(Main instance) {
		super(instance, "Events.RemoveResourcePacksOnJoin");
	}

	@EventHandler(ignoreCancelled = true)
	public void onJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		p.removeResourcePacks();
	}
}
