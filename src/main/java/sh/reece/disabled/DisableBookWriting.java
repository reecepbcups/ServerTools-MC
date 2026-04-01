package sh.reece.disabled;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerEditBookEvent;

public class DisableBookWriting extends ToggleableListener {

	public DisableBookWriting(Main instance) {
		super(instance, "Disabled.DisableBookWriting");
	}

	@EventHandler(ignoreCancelled = true)
	public void onBookWrite(PlayerEditBookEvent e) {
		e.getPlayer().sendMessage(plugin.getConfigUtils().lang("DISABLED_BOOKWRITING"));
		e.setCancelled(true);
	}

}
