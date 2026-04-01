package sh.reece.cmds;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class Donation extends BaseCommand {

	private final List<String> MESSAGE;
	private List<String> FinalMSG = new ArrayList<String>();

	public Donation(Main instance) {
	    super(instance, "Donation", "donation");

	    this.MESSAGE = plugin.getConfig().getStringList("Donation.Message");
	}


	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (noPermission(sender, cmd)) return true;

		// if no arguments given
		if((args.length < 2)) {
			sender.sendMessage(Util.color("&c/Donation <all/player> <IGN> <Package>"));
			return true;
		}

		String announceType = args[0];
		String Reciver = args[1];

		String PackageName = "";
		for (int i = 2; i < args.length; i++) {
			if(i+1 < args.length) {
				PackageName += args[i] + " ";
			} else {
				PackageName += args[i];
			}
        }


		FinalMSG.clear();
		for(String str : MESSAGE) {
			FinalMSG.add(ConfigUtils.replaceVariable(str.replace("%player%", Reciver).replace("%package%", PackageName)));
		}

		if(announceType.equalsIgnoreCase("player")) {
			Player p = Bukkit.getPlayer(Reciver);
			if(p != null && p.isOnline()) {
				sendPlayerLine(p);
			}
		} else {
			for(Player allPlayers : Bukkit.getOnlinePlayers()) {
				sendPlayerLine(allPlayers);
			}
		}

		return true;
	}

	public void sendPlayerLine(Player p) {
		for(String line : FinalMSG) {
			Util.sendCenteredMessage(p, line);
		}
	}

}
