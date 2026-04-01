package sh.reece.core;

import sh.reece.tools.AlternateCommandHandler;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class Nickname implements CommandExecutor, Listener {// ,TabCompleter,Listener {

	private HashMap<UUID, String> nicks = new HashMap<UUID, String>();

	private String Section, Permission, PREFIX, BypassPrefixPerm;
	private final Main plugin;

	public Nickname(Main instance) {
		this.plugin = instance;

		Section = "Core.Nickname";

		if(plugin.enabledInConfig(Section+".Enabled")) {
			plugin.getCommand("nick").setExecutor(this);
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);

			Permission = plugin.getConfig().getString(Section+".Permission");
			PREFIX = plugin.getConfig().getString(Section+".prefix");
			BypassPrefixPerm = plugin.getConfig().getString(Section+".prefixBypass");
		} else {
			AlternateCommandHandler.addDisableCommand("nickname");
			AlternateCommandHandler.addDisableCommand("nick");
		}

	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onChat(AsyncPlayerChatEvent e) {
		// replaces username in format to nickname
		// {prefix}%1$s{suffix}%2$s
		// 
		UUID uuid = e.getPlayer().getUniqueId();

		if (nicks.containsKey(uuid)) {

			// No prefix for staff members
			String output = PREFIX+nicks.get(uuid);
			if(e.getPlayer().hasPermission(BypassPrefixPerm)){
				output = nicks.get(uuid);
			}

			String updatedFormat = e.getFormat().replace("%1$s", output);
			e.setFormat(updatedFormat);
		}

	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (!sender.hasPermission(Permission)) {
			Util.coloredMessage(sender, "&cYou do not have access to &n/" + label + "&c.");
			return true;
		}

		if (args.length == 0) {
			sender.sendMessage(Util.color("&fUsage: &c/" + label + " <nickname/off>"));

		} else if (args.length >= 1) {

			Player p = (Player) sender;
			String newNickname = Util.color(args[0]);

			String message;
			if(newNickname.length() < 3){
				message = "&c[!] Your nickname is too short";

			} else if(newNickname.length() > 20){
				message = "&c[!] Your nickname is too long";

			} else if(doesPlayerHaveSameName(newNickname)){
				message = "&c[!] Your nickname is already in use";

			} else { // success, time to change nickname

				// back to normal name
				if(args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("reset")){
					nicks.remove(p.getUniqueId());
					newNickname = p.getName();
					message = "&a[+] Your nickname has been reset back to normal";

				// Change nickname
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
	

	private boolean doesPlayerHaveSameName(String nick){
		boolean value = false;

		// if another nickname
		// if(nicks.keySet().contains(nick)){
		// 	value = true;
		// }

		// if online player
		for(Player online : Bukkit.getOnlinePlayers()){
			if(online.getName().equalsIgnoreCase(nick)){
				value = true;
			}
		}

		return value;
	}
}
