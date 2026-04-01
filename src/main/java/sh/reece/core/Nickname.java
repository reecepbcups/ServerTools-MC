package sh.reece.core;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class Nickname extends BaseCommand implements Listener {

	private HashMap<UUID, String> nicks = new HashMap<UUID, String>();
	private String PREFIX, BypassPrefixPerm;

	public Nickname(Main instance) {
		super(instance, "Core.Nickname", "nick", "nickname");
		if (isEnabled()) {
			PREFIX = plugin.getConfig().getString(section + ".prefix");
			BypassPrefixPerm = plugin.getConfig().getString(section + ".prefixBypass");
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onChat(AsyncPlayerChatEvent e) {
		UUID uuid = e.getPlayer().getUniqueId();

		if (nicks.containsKey(uuid)) {
			String output = PREFIX + nicks.get(uuid);
			if (e.getPlayer().hasPermission(BypassPrefixPerm)) {
				output = nicks.get(uuid);
			}

			String updatedFormat = e.getFormat().replace("%1$s", output);
			e.setFormat(updatedFormat);
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (noPermission(sender, cmd)) return true;

		if (args.length == 0) {
			sender.sendMessage(Util.color("&fUsage: &c/" + label + " <nickname/off>"));

		} else if (args.length >= 1) {

			Player p = (Player) sender;
			String newNickname = Util.color(args[0]);

			String message;
			if (newNickname.length() < 3) {
				message = "&c[!] Your nickname is too short";

			} else if (newNickname.length() > 20) {
				message = "&c[!] Your nickname is too long";

			} else if (doesPlayerHaveSameName(newNickname)) {
				message = "&c[!] Your nickname is already in use";

			} else {
				if (args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("reset")) {
					nicks.remove(p.getUniqueId());
					newNickname = p.getName();
					message = "&a[+] Your nickname has been reset back to normal";

				} else {
					nicks.put(p.getUniqueId(), newNickname);
					message = "&a[+] Your nickname has been changed to &f" + newNickname;
				}

				p.setDisplayName(newNickname);
				p.setPlayerListName(newNickname);
			}

			Util.coloredMessage(p, message);
		}
		return true;
	}

	private boolean doesPlayerHaveSameName(String nick) {
		boolean value = false;

		for (Player online : Bukkit.getOnlinePlayers()) {
			if (online.getName().equalsIgnoreCase(nick)) {
				value = true;
			}
		}

		return value;
	}
}
