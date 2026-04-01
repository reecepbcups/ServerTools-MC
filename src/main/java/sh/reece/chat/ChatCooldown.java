package sh.reece.chat;

import java.util.Date;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class ChatCooldown implements Listener, CommandExecutor {

	private static Main plugin;
	//public Boolean ChatEnabled;
	public String NoCooldownPerm, CooldownMSG, CommandPermission;
	public Integer CooldownSeconds;
	public Boolean Enabled;
	private HashMap<String, Date> ChatCooldown;
	//private ConfigUtils configUtils;

	public ChatCooldown(Main instance) {
        plugin = instance;



        if (plugin.enabledInConfig("Chat.ChatCooldown.Enabled")) {
			//configUtils = plugin.getConfigUtils();

			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
			plugin.getCommand("chatcooldown").setExecutor(this);


			this.NoCooldownPerm = plugin.getConfig().getString("Chat.ChatCooldown.BypassCooldown");
	    	this.CooldownSeconds = plugin.getConfig().getInt("Chat.ChatCooldown.SecondsCooldown");

	    	this.CooldownMSG = plugin.getConfig().getString("Chat.ChatCooldown.Message");
	    	this.CommandPermission = plugin.getConfig().getString("Chat.ChatCooldown.CommandPermission");

	    	this.Enabled = true;

	    	this.ChatCooldown = new HashMap<String, Date>();



		}
	}


	@EventHandler(ignoreCancelled = true)
	public void onAsyncPlayerChat(AsyncPlayerChatEvent e) {

		if(!e.getPlayer().hasPermission(NoCooldownPerm) && Enabled){
			if(!(Util.cooldown(ChatCooldown, CooldownSeconds, e.getPlayer().getName(), CooldownMSG))) {
				e.setCancelled(true);
			}
		}

	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if(!(cmd.getName().equalsIgnoreCase("chatcooldown") || cmd.getName().equalsIgnoreCase("cooldown"))) {
			return true;
		}


		if (!sender.hasPermission(CommandPermission)) {
			sender.sendMessage(Util.color("&cNo Permission to use the chatcooldown command"));
			return true;
		}

		if(args.length == 0) {
			sender.sendMessage(Util.color("Options: SetCooldown, Toggle"));
		}

		if (args.length >= 1) {
			switch (args[0].toLowerCase()) {
			case "settime":
			case "setcooldown":
			case "set":
				if(!(args.length >= 2)) {
					sender.sendMessage("/" + cmd.getName() + " " + args[0] + " <number>");
					return true;
				}

				if(Util.isInt(args[1])) {
					sender.sendMessage("Set new cooldown for chat to: " + args[1] + " seconds");
					plugin.getConfig().set("Chat.ChatCooldown.SecondsCooldown", Integer.parseInt(args[1]));
					plugin.saveConfig();
					CooldownSeconds = Integer.parseInt(args[1]);
					return true;
				}

				sender.sendMessage("\"" + args[1] +"\" does not seem to be an integer!" );


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
