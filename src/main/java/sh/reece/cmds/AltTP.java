package sh.reece.cmds;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class AltTP extends BaseCommand {

	private FileConfiguration alttpconfig;
	private String FILENAME;
	private List<Player> queue = new ArrayList<Player>();

	public AltTP(Main instance) {
		super(instance, "Commands.AltTP", "alt");

		if (isEnabled()) {
			configUtils.createDirectory("DATA");
			FILENAME = File.separator + "DATA" + File.separator + "AltTP.yml";
			configUtils.createFile(FILENAME);
			alttpconfig = configUtils.getConfigFile(FILENAME);
		}
	}

	private static List<String> possibleArugments = new ArrayList<String>();
	private static List<String> result = new ArrayList<String>();
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {

		if(possibleArugments.isEmpty()) {
			possibleArugments.add("link");
		    possibleArugments.add("unlink");
		    possibleArugments.add("accept");
		    possibleArugments.add("tp");
		    possibleArugments.add("alts");
		}
		result.clear();
		if(args.length == 1) {
			for(String a : possibleArugments) {
				if(a.toLowerCase().startsWith(args[0].toLowerCase())) {
					result.add(a);
				}
			}
			return result;
		}
		return null;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player p = (Player) sender;

		if(args.length == 0) {
			sendHelpMenu(p);
			return true;
		}

		if(args[0].equalsIgnoreCase("alts")) {
			List<String> alts = alttpconfig.getStringList(p.getUniqueId()+".accounts");
			List<String> altNames = new ArrayList<String>();

			for(String alt : alts) {
				Player altPlayer = Bukkit.getPlayer(UUID.fromString(alt));
				altNames.add(altPlayer != null ? altPlayer.getName() : alt);
			}
			Util.coloredMessage(p, "&f&lYour Alts:");
			Util.coloredMessage(p, altNames.toString());
			return true;
		}

		if(args.length <2) {
			sendHelpMenu(p);
			return true;
		}

		Player other = Bukkit.getPlayerExact(args[1]);
		if(other == null) {
			Util.coloredMessage(p, "&4[!] &cPlayer &n" + args[1] + "&c is not online");
			return true;
		}
		if(p.getUniqueId() == other.getUniqueId()) {
			Util.coloredMessage(p, "&c[!] You can not link yourself as your own alt.");
			return true;
		}


		switch(args[0]){

		case "link":
			if(doesPlayerOwnAlt(p, other)) {
				Util.coloredMessage(p, "&cYou have already linked this alt.");
				return true;
			}

			queue.add(other);

			Util.coloredMessage(other, "\n&a[!] " + p.getName() + " wants to link you as an alt!");
			Util.coloredMessage(other, "&f&o    (( /alt accept " + p.getName() + " ))\n");

			Util.coloredMessage(p, "\n&a[!] Sent link reqest to alt\n ");

			return true;

		case "accept":
			queue.remove(p);
			String masteruuid = other.getUniqueId().toString();

			if(!alttpconfig.contains(masteruuid)) {
				alttpconfig.set(masteruuid+".name", other.getName());
				alttpconfig.set(masteruuid+".accounts", new ArrayList<String>());
			}

			List<String> slaves = alttpconfig.getStringList(masteruuid+".accounts");
			slaves.add(p.getUniqueId().toString());
			alttpconfig.set(masteruuid+".accounts", slaves);
			configUtils.saveConfig(alttpconfig, FILENAME);

			Util.coloredMessage(other, "&a[!] " + p.getName() + " has linked to you!");
			Util.coloredMessage(p, "&a[!] Linked as an alt to " + other.getName());
			return true;

		case "tp":
			if(alttpconfig.getStringList(p.getUniqueId()+".accounts").contains(other.getUniqueId().toString())) {
				Util.coloredMessage(p, "&aTeleporting alt...");
				other.teleport(p.getLocation());
			} else {
				Util.coloredMessage(p, "You do not seem you own the alt " + other.getName());
			}
			return true;

		case "unlink":
			if(alttpconfig.getStringList(p.getUniqueId()+".accounts").contains(other.getUniqueId().toString())) {
				List<String> accounts = alttpconfig.getStringList(p.getUniqueId()+".accounts");
				accounts.remove(other.getUniqueId().toString());
				alttpconfig.set(p.getUniqueId()+".accounts", accounts);
				configUtils.saveConfig(alttpconfig, FILENAME);
			} else {
				Util.coloredMessage(p, "You do not seem you own the alt " + other.getName()+". Can not remove");
			}
			return true;

		default:
			sendHelpMenu(p);
			return true;
		}
	}

	public Boolean doesPlayerOwnAlt(Player master, Player alt) {
		if(alttpconfig.getStringList(master.getUniqueId()+".accounts")
				.contains(alt.getUniqueId().toString())) {
			return true;
		}
		return false;
	}

	public void sendHelpMenu(Player p) {
		Util.coloredMessage(p, "&f/alt unlink username");
		Util.coloredMessage(p, "&f/alt link username");
		Util.coloredMessage(p, "&f/alt accept username");
		Util.coloredMessage(p, "&f/alt tp username");
		Util.coloredMessage(p, "&f/alt alts");
	}
}
