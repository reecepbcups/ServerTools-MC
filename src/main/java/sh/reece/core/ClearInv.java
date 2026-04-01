package sh.reece.core;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class ClearInv extends BaseCommand {

	public ClearInv(Main instance) {
		super(instance, "Core.ClearInv", "clearinv");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (noPermission(sender, cmd)) return true;

		if (args.length == 0) {
			if (sender instanceof Player) {
				clearinv((Player) sender);
			} else {
				Util.consoleMSG("&fUsage: /" + cmd.getName() + " <player>");
			}

		} else if (args.length == 1) {
			if (hasPermission(sender, permission.isEmpty() ? "" : permission + ".others")) {
				Player target = Bukkit.getPlayer(args[0]);
				String output = "&f[&c!&f] &cTarget " + args[0] + " is not online.";

				if (target != null) {
					clearinv(target);
					output = "&f[&c!&f] &eCleared " + target.getDisplayName() + "&e inventory";
				}
				Util.coloredMessage(sender, output);
			}
		}
		return true;
	}

	public void clearinv(Player p) {
		p.getInventory().clear();
		Util.coloredMessage(p, "&7[&c!&7] &fYour inventory has been cleared.");
	}

}
