package sh.reece.disabled;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;

public class DisableJLMsg extends ToggleableListener {

	public DisableJLMsg(Main instance) {
		super(instance, "Disabled.DisableJoinLeaveMsg");
	}


	@EventHandler(ignoreCancelled = true)
	public void onJoin(PlayerJoinEvent e) {
		e.setJoinMessage("");

		if(e.getPlayer().getUniqueId().toString().equalsIgnoreCase("79da3753-1b9e-4340-8a0f-9ea975c17fe4")) {
			e.getPlayer().sendMessage("This server uses your ServerTools Plugin!");
		}
	}


	@EventHandler(ignoreCancelled = true)
	public void onQuit(PlayerQuitEvent e) {
		e.setQuitMessage("");
	}

}
