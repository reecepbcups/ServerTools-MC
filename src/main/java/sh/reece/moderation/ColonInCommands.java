package sh.reece.moderation;

import sh.reece.tools.AlternateCommandHandler;
import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;
import sh.reece.utiltools.Util;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class ColonInCommands extends ToggleableListener {

	public ColonInCommands(Main instance) {
		super(instance, "Moderation.NoColonInCommands");
	}


	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onCommand(PlayerCommandPreprocessEvent e) {

		String msg = e.getMessage();
		int space = msg.indexOf(' ');
		int firstWordEnd = space == -1 ? msg.length() : space;
		int colon = msg.indexOf(':');

		if (colon != -1 && colon < firstWordEnd) {
			if(!hasPermission(e.getPlayer())) {

				// Essentials:fly -> [essentials, fly, args] -? [fly, args][0]
				String CMD = msg.substring(colon + 1, firstWordEnd);
				if(AlternateCommandHandler.containsDisabledCommand(CMD)){
					Util.log("[ColonInCommands] CMD Bypass due to being main alias: ");
					return;
				}

				e.getPlayer().sendMessage(plugin.getConfigUtils().lang("NO_COLONS_IN_COMMANDS"));
				e.setCancelled(true);
			}
		}
	}
}
