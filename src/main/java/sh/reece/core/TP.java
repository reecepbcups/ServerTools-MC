package sh.reece.core;

import sh.reece.tools.AlternateCommandHandler;
import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TP implements CommandExecutor{//,TabCompleter,Listener {

	String Section, TP, TPA, TPHere;
	private final Main plugin;
	
	// to, from
	private final Map<UUID, UUID> currentRequest = new HashMap<>();
	private ConfigUtils configUtils;
	
	public TP(Main instance) {
		plugin = instance;


		Section = "Core.Teleport";        

		// https://essinfo.xeya.me/permissions.html
		if(plugin.enabledInConfig(Section+".Enabled")) {
			configUtils = plugin.getConfigUtils();

			plugin.getCommand("teleport").setExecutor(this);
			
			TP = plugin.getConfig().getString(Section+".Permissions.TP");
			TPA = plugin.getConfig().getString(Section+".Permissions.TPA");
			TPHere = plugin.getConfig().getString(Section+".Permissions.TPHere");
		} else {
			AlternateCommandHandler.addDisableCommand("tp");
			AlternateCommandHandler.addDisableCommand("tpa");
			AlternateCommandHandler.addDisableCommand("tphere");
			AlternateCommandHandler.addDisableCommand("teleport");			
		}

	}


	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player p = (Player) sender;
		
		
		switch (label.toLowerCase()) {
		// done here as its just a single command, and has no args
		case "tpaccept":
		case "tpyes":
			if(currentRequest.containsKey(p.getUniqueId())) {
				// accepted teleport request
				Player from = Bukkit.getPlayer(currentRequest.get(p.getUniqueId()));
				if(from != null) {
					Util.coloredMessage(from, configUtils.lang("TELEPORT_HASACCEPTED").replace("%player%", p.getName()));
					from.teleport(p);
				}
				Util.coloredMessage(p, configUtils.lang("TELEPORT_ACCEPTED").replace("%player%", from != null ? from.getName() : "?"));
				currentRequest.remove(p.getUniqueId());

			} else {
				Util.coloredMessage(p, configUtils.lang("TELEPORT_NOREQUEST"));
			}
			return true;


		case "tpcancel":
		case "tpdeny":
		case "tpno":
			if(currentRequest.containsKey(p.getUniqueId())) {
				Player from = Bukkit.getPlayer(currentRequest.get(p.getUniqueId()));
				if(from != null) {
					Util.coloredMessage(from, configUtils.lang("TELEPORT_HASDENIED").replace("%player%", p.getName()));
				}
				Util.coloredMessage(p, configUtils.lang("TELEPORT_DENIED").replace("%player%", from != null ? from.getName() : "?"));
				currentRequest.remove(p.getUniqueId());
			} else {
				Util.coloredMessage(p, configUtils.lang("TELEPORT_NOREQUEST"));
			}
			return true;	
			
		default:
			break;	
		}
		
		
		if (args.length == 0) {										
			Util.coloredMessage(p, "&fUsage: &c/"+label+" <player>");
		} else {
			Player target = Bukkit.getPlayer(args[0]);
			String output = "&cPlayer "+args[0]+" is not online.";
			
			if(target == p) {
				output = configUtils.lang("TELEPORT_SELF");
			} else if(target != null) {			
				// Util.consoleMSG(target.getName());
				
				switch (label.toLowerCase()) {
				case "tp":
				case "tpo":	
					if(checkPerm(p, label, TP)) {
						output = configUtils.lang("TELEPORT_TO").replace("%player%", args[0]);
						p.teleport(target);
					} else {
						return true;
					}
					break;

				case "tphere":
					if(checkPerm(p, label, TPHere)) {
						output = "&aTeleported &f" + args[0] + " &ato &fYou";					
						target.teleport(p);
						Util.coloredMessage(target,configUtils.lang("TELEPORT_TO").replace("%player%", p.getName()));
					} else {
						return true;
					}
					break;

				//  TPA TPACCEPT TPDENY
				case "tpa":					
					// check cooldown here, "You must wait a " + cooldown + " second cooldown in between teleport requests!"					
					if(checkPerm(p, label, TPA)) {
						sendRequest(p, target);	
						return true;
					} 					
					break;			
					
				default:
					break;
				}	
				
			}
			Util.coloredMessage(p, output);
			
		}
		return true;
	}	
	
	
	private void sendRequest(Player from, Player to) {
		Util.coloredMessage(from, configUtils.lang("TELEPORT_SENT_REQUEST").replace("%player%", to.getName()));
		
		Util.coloredMessage(to, configUtils.lang("TELEPORT_GOT_REQUEST1").replace("%player%", from.getName()));
		Util.coloredMessage(to, configUtils.lang("TELEPORT_GOT_REQUEST2").replace("%player%", from.getName()));
		currentRequest.put(to.getUniqueId(), from.getUniqueId());
		
	}
	
	public boolean killRequest(Player p) {
	    if (currentRequest.containsKey(p.getUniqueId())) {
	      Player loser = Bukkit.getPlayer(currentRequest.get(p.getUniqueId()));
	      if (loser != null) {
			  loser.sendMessage(configUtils.lang("TELEPORT_TIMEOUT"));
		  }
	      currentRequest.remove(p.getUniqueId());
	      return true;
	    }
	    return false;
	  }
	
	public boolean checkPerm(Player p, String CMD, String perm) {
		if (!p.hasPermission(perm)) {
			Util.coloredMessage(p, "&cYou do not have access to &n/"+CMD+"&c.");
			return false;
		} 		
		return true;
	}
	
}
