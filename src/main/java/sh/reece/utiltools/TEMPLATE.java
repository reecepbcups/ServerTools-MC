package sh.reece.utiltools;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;

public class TEMPLATE extends BaseCommand implements Listener {

	public TEMPLATE(Main instance) {
		super(instance, "Chat.TEMPLATE_OPTION", "COMMAND_NAME");
	}

	@EventHandler
	public void playerColoredChatEvent(AsyncPlayerChatEvent e) {
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (noPermission(sender, cmd)) return true;

		Player p = playerOrNull(sender);
		if (p == null) return true;

		if (args.length == 0) {
			sendHelpMenu(p);
			return true;
		}

		switch (args[0]) {
			case "clear":
				return true;
			case "set":
				return true;
			default:
				sendHelpMenu(p);
				return true;
		}
	}

	public void sendHelpMenu(Player p) {
		Util.coloredMessage(p, "&f/command &7<args>");
	}
}
