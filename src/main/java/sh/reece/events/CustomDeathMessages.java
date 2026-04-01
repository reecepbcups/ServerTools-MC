package sh.reece.events;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class CustomDeathMessages implements Listener, CommandExecutor {

	private Boolean ShowDeathMessages = false;
	private String permission, deathFormat;
	private boolean hasDeathFormat;

	private Main plugin;
	public CustomDeathMessages(Main instance) {
		plugin = instance;

		if (plugin.enabledInConfig("Chat.CustomDeathMessages.Enabled")) {
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);

			permission = "toggledeath.use";
			deathFormat = plugin.getConfig().getString("Chat.CustomDeathMessages.message");
			hasDeathFormat = deathFormat != null && deathFormat.length() > 0;

			plugin.getCommand("toggledeathmessages").setExecutor(this);
			ShowDeathMessages = true;
		}
	}


	@EventHandler(ignoreCancelled = true)
	public void onKill(PlayerDeathEvent e) {
		String msg = "";
		if(ShowDeathMessages && hasDeathFormat) {
			msg = Util.color(deathFormat.replace("%message%", e.getDeathMessage()));
		}

		e.setDeathMessage(msg);

	}


	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (!(sender.hasPermission(permission))) {
			sender.sendMessage(Util.color("&cNo Permission to use ToggleDeath :("));
			return true;
		}

		if (args.length >= 0) {
			Player p = (Player) sender;
			ShowDeathMessages = !ShowDeathMessages;
			Util.coloredMessage(p, "&fShow Death Messages: " + ShowDeathMessages);
		}

		return true;
	}


}
