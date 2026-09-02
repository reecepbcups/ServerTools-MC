package sh.reece.events;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;
import sh.reece.utiltools.Schedulers;
import sh.reece.utiltools.Util;

public class OnJoinCommands extends ToggleableListener {

	private List<String> FirstJoinCMDS, PlayerRunOnJoin;
	private Boolean isFirstJoinEnabled, isPlayerRunEnabled;

	public OnJoinCommands(Main instance) {
		super(instance, "Misc.OnJoinCommands");

		if (isEnabled()) {
			String section = "Misc.OnJoinCommands";
			FirstJoinCMDS = instance.getConfig().getStringList(section + ".FirstUniqueJoin.CMDS");
			PlayerRunOnJoin = instance.getConfig().getStringList(section + ".PlayerRunCommands.CMDS");

			isFirstJoinEnabled = instance.getConfig().getString(section + ".FirstUniqueJoin.Enabled").equalsIgnoreCase("true");
			isPlayerRunEnabled = instance.getConfig().getString(section + ".PlayerRunCommands.Enabled").equalsIgnoreCase("true");
		} else {
			isFirstJoinEnabled = false;
			isPlayerRunEnabled = false;
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void PlayerCommand(PlayerJoinEvent event) {
		Player p = (Player) event.getPlayer();

		// First Join commands enabled & Player has not played before
		if (isFirstJoinEnabled) {
			if (!(p.hasPlayedBefore())) {
				// console commands - global region scheduler
				Schedulers.globalLater(plugin, () -> {
					for (String cmd : FirstJoinCMDS) {
						Util.console(cmd.replace("%player%", p.getName()));
					}
				}, 10L);
			}
		}

		if (isPlayerRunEnabled) {
			// the player runs these, so pin to their entity scheduler
			Schedulers.entityLater(plugin, p, () -> {
				for (String command : PlayerRunOnJoin) {
					p.performCommand(command.replace("%player%", p.getName()));
				}
			}, 10L);
		}
	}
}
