package sh.reece.events;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class CustomDeathMessages extends BaseCommand implements Listener {

	private Boolean ShowDeathMessages = false;
	private String deathFormat;
	private boolean hasDeathFormat;

	public CustomDeathMessages(Main instance) {
		super(instance, "Chat.CustomDeathMessages", "toggledeathmessages");

		if (isEnabled()) {
			deathFormat = instance.getConfig().getString("Chat.CustomDeathMessages.message");
			hasDeathFormat = deathFormat != null && deathFormat.length() > 0;
			ShowDeathMessages = true;
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onKill(PlayerDeathEvent e) {
		String msg = "";
		if (ShowDeathMessages && hasDeathFormat) {
			msg = Util.color(deathFormat.replace("%message%", e.getDeathMessage()));
		}

		e.setDeathMessage(msg);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (noPermission(sender, cmd)) return true;

		if (args.length >= 0) {
			Player p = (Player) sender;
			ShowDeathMessages = !ShowDeathMessages;
			Util.coloredMessage(p, "&fShow Death Messages: " + ShowDeathMessages);
		}

		return true;
	}
}
