package sh.reece.chat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class ChatCooldown extends BaseCommand implements Listener {

	public String NoCooldownPerm, CooldownMSG;
	public int CooldownSeconds;
	public volatile boolean Enabled;
	private Map<String, Long> ChatCooldownMap;

	public ChatCooldown(Main instance) {
		super(instance, "Chat.ChatCooldown", "chatcooldown");

		if (isEnabled()) {
			this.NoCooldownPerm = instance.getConfig().getString("Chat.ChatCooldown.BypassCooldown");
			this.CooldownSeconds = instance.getConfig().getInt("Chat.ChatCooldown.SecondsCooldown");

			this.CooldownMSG = instance.getConfig().getString("Chat.ChatCooldown.Message");

			this.Enabled = true;
			this.ChatCooldownMap = new ConcurrentHashMap<>();
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onAsyncPlayerChat(AsyncPlayerChatEvent e) {

		if (!e.getPlayer().hasPermission(NoCooldownPerm) && Enabled) {
			if (!(Util.cooldown(ChatCooldownMap, CooldownSeconds, e.getPlayer().getName(), CooldownMSG))) {
				e.setCancelled(true);
			}
		}
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (noPermission(sender, cmd)) return true;

		if (args.length == 0) {
			sender.sendMessage(Util.color("Options: SetCooldown, Toggle"));
		}

		if (args.length >= 1) {
			switch (args[0].toLowerCase()) {
				case "settime":
				case "setcooldown":
				case "set":
					if (!(args.length >= 2)) {
						sender.sendMessage("/" + cmd.getName() + " " + args[0] + " <number>");
						return true;
					}

					if (Util.isInt(args[1])) {
						sender.sendMessage("Set new cooldown for chat to: " + args[1] + " seconds");
						plugin.getConfig().set("Chat.ChatCooldown.SecondsCooldown", Integer.parseInt(args[1]));
						plugin.saveConfig();
						CooldownSeconds = Integer.parseInt(args[1]);
						return true;
					}

					sender.sendMessage("\"" + args[1] + "\" does not seem to be an integer!");

					break;

				case "toggle":
					sender.sendMessage("Toggled ChatCooldown: " + Enabled);
					Enabled = !Enabled;
					break;

				default:
					sender.sendMessage(Util.color("Options: SetCooldown, Toggle"));
					break;
			}
		}

		return true;
	}
}
