package sh.reece.core.warp;

import sh.reece.tools.AlternateCommandHandler;
import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class WarpCMD implements CommandExecutor, TabCompleter {

	String Section;
	private final Main plugin;

	String setWarpPerm, delWarpPerm, viewWarpPerm, warpOtherPlayerPerm;

	// name, warp_data
	private static Map<String, Warp> warps = new HashMap<>();
	
	public WarpCMD(Main instance) {
		plugin = instance;
		
		
		Section = "Core.Warps";        

		// https://essinfo.xeya.me/permissions.html
		if(plugin.getConfigUtils().enabledInConfig(Section+".Enabled")) {
			// configUtils = plugin.getConfigUtils();
			plugin.getCommand("warp").setExecutor(this);
			plugin.getCommand("warp").setTabCompleter(this);

			ConfigUtils.getInstance().createConfig("Warps.yml");
			loadWarpsFromConfig();

			setWarpPerm = plugin.getConfig().getString(Section + ".DeleteWarpPerm");
			delWarpPerm = plugin.getConfig().getString(Section + ".SetWarpPerm");
			viewWarpPerm = plugin.getConfig().getString(Section + ".ViewWarpPerm");
			warpOtherPlayerPerm = plugin.getConfig().getString(Section + ".WarpOtherPlayToWarpPerm");

			if(setWarpPerm == null) {
				setWarpPerm = "tools.setwarp";
			}
			if(delWarpPerm == null) {
				delWarpPerm = "tools.delwarp";
			}
			if(viewWarpPerm == null) {
				viewWarpPerm = "tools.viewwarp";
			}
			if(warpOtherPlayerPerm == null) {
				warpOtherPlayerPerm = "tools.warpother";
			}

		} else {
			AlternateCommandHandler.addDisableCommand("warp");
			AlternateCommandHandler.addDisableCommand("setwarp");
			AlternateCommandHandler.addDisableCommand("addwarp");
			AlternateCommandHandler.addDisableCommand("delwarp");
			AlternateCommandHandler.addDisableCommand("remwarp");
			AlternateCommandHandler.addDisableCommand("warps");
			AlternateCommandHandler.addDisableCommand("warpinfo");
		}
		
	}
	
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {		
		// /warp, /warps, /setwarp <name>, /delwarp <name>

		// check if the label name is warps
		if(label.equalsIgnoreCase("warps")){
			int page = 0;
			if(args.length > 0) {
				if(Util.isInt(args[0])){
					page = Integer.parseInt(args[0]);
				}
			}
			showWarps(sender, page);
			return true;
		}


		if(label.equalsIgnoreCase("setwarp") || label.equalsIgnoreCase("addwarp")) {
			if(!sender.hasPermission(setWarpPerm)) {
				Util.coloredMessage(sender, "&cYou do not have permission to use this command.");
				return true;
			}

			if(args.length > 0) {
				Player p = (Player) sender;				
				setWarp(p, args);
			} else {
				Util.coloredMessage(sender, "Usage: /setwarp <warp> [permission]");
			}
			return true;			
		}

		if(label.equalsIgnoreCase("delwarp") || label.equalsIgnoreCase("remwarp")) {
			if(!sender.hasPermission(delWarpPerm)) {
				Util.coloredMessage(sender, "&cYou do not have permission to use this command.");
				return true;
			}

			if(args.length > 0) {				
				delwarp(sender, args[0]);
			} else {
				Util.coloredMessage(sender, "Usage: /delwarp <warp>");
			}
			return true;			
		}

		if(label.equalsIgnoreCase("warpinfo")) {
			if(!sender.hasPermission(viewWarpPerm)) {
				Util.coloredMessage(sender, "&cYou do not have permission to use this command.");
				return true;
			}

			if(args.length >= 1) {
				String warpName = args[0];
				Warp warpData = warps.get(warpName);

				if(warpData == null) {
					Util.coloredMessage(sender, "&cWarp '"+warpName+"' not found.");
					return true;
				}

				String perm = warpData.getPermission();
				Location loc = warpData.getLocation();

				Util.coloredMessage(sender, "&f&lWarp&7: &e"+warpName);
				if(perm != null && perm.length() > 0) {
					Util.coloredMessage(sender, "&f&lPermission&7: &e"+ perm);
				}
				Util.coloredMessage(sender, "&f&lLocation&7: &e"+ loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());

			} else {
				Util.coloredMessage(sender, "Usage: /warpinfo <warp>");
			}
			return true;			
		}
		

		// if args is 0 OR they enter /warp #, send player warp list of keys, where # is the page
		if(args.length == 0) {			
			showWarps(sender, 0);

		} else if(args.length >= 1 ){

			if(Util.isInt(args[0])) {
				showWarps(sender, Integer.parseInt(args[0]));

			} else {
				Player playerToTeleport = (Player) sender;
				String fromWho = "";

				if(args.length >= 2) {
					if(!playerToTeleport.hasPermission(warpOtherPlayerPerm)){
						Util.coloredMessage(sender, "&cYou do not have permission to teleport other players.");
						return true;
					}

					playerToTeleport = Bukkit.getPlayer(args[1]);
					if(playerToTeleport == null) {
						Util.coloredMessage(sender, "&cPlayer '"+args[1]+"' not found online to warp to that location.");
						return true;
					}
					fromWho = "&7&o(( from " + sender.getName() + " ))";
				}

				Warp warpingTo = warps.get(args[0]);
				if(warpingTo == null) {
					Util.coloredMessage(sender, "&cThis is not a warp location.");
					return true;
				}

				if(playerToTeleport.getName() != sender.getName()) {
					Util.coloredMessage(sender, "&aTeleporting &f"+playerToTeleport.getName()+" &ato warp &f"+args[0]+"&7.");
				}

				teleportPlayer(playerToTeleport, warpingTo, fromWho);
			}
		}
		
		return true;
	}

	private void setWarp(Player p, String[] args){
		String warpName = "";
		String warpPerm = null;

		if(args.length >= 1) {
			warpName = args[0];
			if(args.length >= 2) {
				warpPerm = args[1];
			}
		}

		if(Util.isInt(warpName)) {
			Util.coloredMessage(p, "&cYou can not set a warp as a number.");
			return;
		}

		if(getWarps().contains(warpName)){
			Util.coloredMessage(p, "&cThere is already a warp set as this name!");
			return;
		}

		Warp newWarp = new Warp(warpName, p.getLocation(), warpPerm);
		warps.put(warpName, newWarp);
		newWarp.saveToConfig();		
		Util.coloredMessage(p, "&aWarp set as &n"+warpName+"&a.");
	}

	private void delwarp(CommandSender sender, String name){
		if(!getWarps().contains(name)){
			Util.coloredMessage(sender, "&cThis warp does not exist!");
			return;
		}

		Warp deletingWarp = warps.get(name);
		warps.remove(name);
		deletingWarp.removeFromConfig();
		
		Util.coloredMessage(sender, "&aWarp &n"+name+"&a has been deleted.");
	}

	private void showWarps(CommandSender sender, int page) {

		Util.coloredMessage(sender, "\n&f&lWarps &7&o( "+getWarps().size()+" )");

		StringBuilder sb = new StringBuilder();
		Player player = (Player) sender;

		for(String name : getWarps()) {
			// could add red / green coloring in future based on perms
			Warp w = warps.get(name);
			if(w.getPermission() != null && w.getPermission().length() > 0) {
				if(player.hasPermission(w.getPermission())) {
					sb.append("&f"+name+"&7, ");
				} else {
					sb.append("&c"+name+"&7, ");
				}				
			} else {
				sb.append("&f"+name+"&7, ");
			}

			// sb.append("&f&o"+name+"&7&o, ");
		}

		Util.coloredMessage(sender, sb.toString());
	}


	private void teleportPlayer(Player player, Warp warpingTo, String fromWho) {			

		Location warpLoc = warpingTo.getLocation();

		// if no one sent them (admin), then check perms
		if(warpingTo.getPermission() != null && fromWho.length() == 0) {
			if(!player.hasPermission(warpingTo.getPermission())) {
				Util.coloredMessage(player, "&cYou do not have access to '"+warpingTo.getName()+"'. &o(" + warpingTo.getPermission() + ")");
				return;
			}
		}
		
		player.teleport(warpLoc);
		Util.coloredMessage(player, "&2[!] &aTeleported to &n"+warpingTo.getName()+"&2. " + fromWho);		
	}

	public static void loadWarpsFromConfig() {
		FileConfiguration configWarps = ConfigUtils.getInstance().getConfigFile("Warps.yml");

		if(configWarps == null || configWarps.getKeys(false).size() == 0) {
			return;
		}

		for(String wName : configWarps.getKeys(false)){
			// Util.log(wName);
			String perm = configWarps.getString(wName + ".permission");
			String loc = configWarps.getString(wName + ".location");

			Warp newWarp = new Warp(wName, Util.stringToLocation(loc), perm);
			warps.put(wName, newWarp);
		}
	}

	private Set<String> getWarps(){
		return warps.keySet();
	}


	// private List<String> possibleArugments = new ArrayList<String>();
	private List<String> result = new ArrayList<String>();
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {		
		// if(possibleArugments.isEmpty()) {
		// 	possibleArugments.add("create");
		// }
		result.clear();

		if(label.equalsIgnoreCase("warp") || label.equalsIgnoreCase("warpinfo")) {
			if(args.length == 1) {			
				for(String a : getWarps()) {
					// only show warps in tab complete which the user has access to even view
					String perm = warps.get(a).getPermission();
					if(perm != null) {
						if(sender.hasPermission(perm)) {
							result.add(a);
						}
					} else {
						// if there is no permission set by default
						result.add(a);
					}
				}
				return result;
			}	
		}
		
		if(label.equalsIgnoreCase("setwarp") || label.equalsIgnoreCase("addwarp")) {
			if(args.length == 1) {			
				result.add("<warp-name>");
				return result;

			} else if(args.length == 2) {			
				result.add("[permission]");
				return result;
			}
		}

		return null;
	}
}
