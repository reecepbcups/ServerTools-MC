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


	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onCommand(PlayerCommandPreprocessEvent e) {

		if(e.isCancelled()) {
			return;
		}

		//if (e.getMessage().split(" ")[0].contains(":")) {
		if(e.getMessage().indexOf(":") != -1) {
			if(!hasPermission(e.getPlayer())) {

				// Essentials:fly -> [essentials, fly, args] -? [fly, args][0]
				String CMD = e.getMessage().split(":")[1].split(" ")[0];
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
