package sh.reece.core;

import sh.reece.tools.AlternateCommandHandler;
import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public class Messaging implements CommandExecutor, Listener, TabCompleter { //,,Listener {

	// /r, /reply
	// /togglemsg
	// /disableMessaging
	// /socialspy
	
	// add message formats
	// add commandSpy here?
	private final String Section;
	private String msgPerm;
	private String replyPerm;
	private String socialSpyPerm;
	private String ToggleMSGPerm;
	private String DisableMessagingPerm;
	private String staffBypassPerm;
	private String FORMAT_SEND, FORMAT_FROM;
	private final Main plugin;
	
	private final List<UUID> socialSpyEnabled = new ArrayList<>();
	private final List<UUID> toggledMessages = new ArrayList<>();
	private final Map<String, String> lastMessage = new HashMap<>();
	
	private boolean isMessagingDisabled;
	private ConfigUtils configUtils;
	
	public Messaging(Main instance) {
		plugin = instance;
				
		Section = "Core.Messaging";        
		isMessagingDisabled = false; 
		
		// https://essinfo.xeya.me/permissions.html
		if(plugin.enabledInConfig(Section+".Enabled")) {

			configUtils = plugin.getConfigUtils();
			
			plugin.getCommand("reply").setExecutor(this);
			plugin.getServer().getPluginManager().registerEvents(this, plugin);
			//plugin.getCommand("message").setTabCompleter(this);
						
			msgPerm = plugin.getConfig().getString(Section+".Permissions.Message");
			replyPerm = plugin.getConfig().getString(Section+".Permissions.Reply");
			socialSpyPerm = plugin.getConfig().getString(Section+".Permissions.SocialSpy");
			ToggleMSGPerm = plugin.getConfig().getString(Section+".Permissions.ToggleMSG"); 
			DisableMessagingPerm = plugin.getConfig().getString(Section+".Permissions.DisableMessaging");
			staffBypassPerm = plugin.getConfig().getString(Section+".Permissions.StaffBypass");
			
			FORMAT_SEND = plugin.getConfig().getString(Section+".Formats.Send");
			FORMAT_FROM = plugin.getConfig().getString(Section+".Formats.From");	
		} else {
			AlternateCommandHandler.addDisableCommand("reply");
			AlternateCommandHandler.addDisableCommand("message");
		}
		
	}
	
	
	// /r hey whats up
	// checks if args[0] is online, if not, check if they are in the last reply hash.
	// if they are, make player run command /message <playerFromReplyHash> args
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		Player p = (Player) sender;
		
		//Util.consoleMSG("&C&K!!! &bMessaging run &f - " + label);
		
		switch (label) {
		case "m":
		case "msg":
		case "message":
			
			if(isMessagingDisabled == true) {
				Util.coloredMessage(p, configUtils.lang("MESSAGING_DISABLED"));
				return true;
			}
			
			if(checkperm(p, cmd.getName(), msgPerm)){

				if(args.length >= 2) {

					sendMessage(p, args[0], Util.argsToSingleString(1, args));

				} else {
					Util.coloredMessage(p, "Not enough arguments! / help menu here");
					Util.coloredMessage(p, "&cUSAGE: &f/"+label+" <Player> <Message>");
				}
				
			}
			break;

		case "r":
		case "reply":
			if(checkperm(p, label, replyPerm)){
				doReply(p, Util.argsToSingleString(0, args));
			}
			break;	
		
			
			
		case "socialspy":	
			if(checkperm(p, label, socialSpyPerm)){		
				toggleSocialSpy(p);
			}
			break;	
			
		case "disablemessaging":
		case "disablemessage":	
		case "disablemsg":		
			if(checkperm(p, label, DisableMessagingPerm)){		
				isMessagingDisabled = !isMessagingDisabled;
				Util.coloredMessage(p, "&fMessaging is now toggled to: " + isMessagingDisabled);
			}
			break;	
			
		case "togglemsg":
		case "togglemessaging":	
		case "tpm":
		case "togglepms":
			if(checkperm(p, label, ToggleMSGPerm)){		
				toggleMessages(p);
			}
			break;	
			
		default:
			break;
		}

		return true;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		return null;
	}
	
	
	private void sendMessage(Player sender, String targetStr, String msg) {
		Player target = Bukkit.getPlayer(targetStr);		
		if(target == null) {
			// player not online, might be due to doing /r				
			if(lastMessage.containsKey(sender.getName())) {
				target = Bukkit.getPlayer(lastMessage.get(sender.getName()));
			} else {
				Util.coloredMessage(sender, configUtils.lang("MESSAGING_OFFLINE").replace("%target%", targetStr));
				return;
			}					
		}
		
		// staff bypass for disabled messages
		if(toggledMessages.contains(target.getUniqueId())) {

			if(!sender.hasPermission(staffBypassPerm)){

				Util.coloredMessage(sender, configUtils.lang("MESSAGING_IS_TOGGLED").replace("%target%", target.getName()));
				return;

			}
		}
			
		
		String sendName = sender.getName();
		String toName = target.getName();

		Util.coloredMessage(target, FORMAT_FROM.replace("%name%", sendName).replace("%msg%", msg));
		Util.coloredMessage(sender, FORMAT_SEND.replace("%name%", toName).replace("%msg%", msg));

		String SocialSpyMSG = "&7From &b" + sendName + "&7 to &9" + toName + "&7: &3" + msg;
	
		for(UUID uuid : socialSpyEnabled) {
			Player SocialSpyPlayer = Bukkit.getPlayer(uuid);
			Util.coloredMessage(SocialSpyPlayer, SocialSpyMSG);
		}
		
		lastMessage.put(sendName, toName);
		lastMessage.put(toName, sendName);
		
		// play sound for person sent too
		if(plugin.getConfig().getBoolean("Core.Messaging.Sound.Enabled")) {
			String soundSTR = plugin.getConfig().getString("Core.Messaging.Sound.Sound");
			
			try {				
				Sound s = Sound.valueOf(soundSTR);
				target.playSound(target.getLocation(), s, 3.0F, 0.5F);
			} catch (Exception e) {
				Util.consoleMSG(soundSTR + " is not recongized for Core.Messaging.Sound.Sound in ServerTools config.yml");
			}
			
		}		
	}
	
	
	
	@EventHandler(ignoreCancelled = true)
	public void onQuit(PlayerQuitEvent e) {
		String username = e.getPlayer().getName();
		lastMessage.remove(username);
		lastMessage.values().remove(username);
	}
	
	private boolean checkperm(Player p, String CMD, String perm) {
		if (!p.hasPermission(perm)) {
			Util.coloredMessage(p, "&cYou do not have access to &n/" +CMD+"&c.");
			return false;
		} 
		return true;
	}	
	
	private void toggleSocialSpy(Player p) {
		String toggleStatus = "&7SocialSpy &aenabled&7.";
		
		if(socialSpyEnabled.contains(p.getUniqueId())) {
			toggleStatus = "&7SocialSpy &cdisabled&7.";
			socialSpyEnabled.remove(p.getUniqueId());
		} else {					
			socialSpyEnabled.add(p.getUniqueId());
		}					
		Util.coloredMessage(p, toggleStatus);
	}
	
	private void toggleMessages(Player p) {
		String toggleStatus = "&7Receiving Messages &cdisabled&7.";
		
		if(!toggledMessages.contains(p.getUniqueId())) {			
			toggledMessages.add(p.getUniqueId());			
		} else {	
			toggleStatus = "&7Receiving Messages &aenabled&7.";
			toggledMessages.remove(p.getUniqueId());
		}					
		Util.coloredMessage(p, toggleStatus);
	}
	
	private void doReply(Player p, String message) {
		if(lastMessage.containsKey(p.getName())) {
			if(message.length() > 0) {
				sendMessage(p, lastMessage.get(p.getName()), message);
			} else {
				Util.coloredMessage(p, "&c[!] &f/reply <messsage>");
			}
			
			
			// sendMessage(p, lastMessage.get(p.getName()), Util.argsToSingleString(1, args));
		} else {
			Util.coloredMessage(p, configUtils.lang("MESSAGING_NOREPLY"));
		}
	}
	
	
}
