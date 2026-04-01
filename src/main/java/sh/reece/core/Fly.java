package sh.reece.core;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Fly extends BaseCommand {

	public Fly(Main instance) {
		super(instance, "Core.Fly", "fly");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (noPermission(sender, cmd)) return true;

		if (args.length == 0) {
			if (sender instanceof Player) {
				toggleFlying((Player) sender);
			} else {
				Util.consoleMSG("&fUsage: &c/fly <player>");
			}
			return true;
		}

		if (args.length == 1) {
			if (hasPermission(sender, permission.isEmpty() ? "" : permission + ".others")) {
				Player target = resolveTarget(sender, args, cmd);
				if (target != null) {
					toggleFlying(target);
					Util.coloredMessage(sender, "&f[&c!&f] &eToggled " + args[0] + " flight mode to &e" + target.getAllowFlight());
				}
			}
		}

		return true;
	}

	public void toggleFlying(Player p) {
		if (p.getAllowFlight()) {
			p.setAllowFlight(false);
			Util.coloredMessage(p, configUtils.lang("FLY_DISABLED").replace("%player%", p.getDisplayName()));
		} else {
			p.setAllowFlight(true);
			Util.coloredMessage(p, configUtils.lang("FLY_ENABLED").replace("%player%", p.getDisplayName()));
		}
	}

}
