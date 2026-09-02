package sh.reece.core;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Schedulers;
import sh.reece.utiltools.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class Heal extends BaseCommand {

	private String HealPerm, HealOthersPerm, FeedPerm;

	public Heal(Main instance) {
		super(instance, "Core.Heal", "heal", "feed", "healall");
		if (isEnabled()) {
			HealPerm = plugin.getConfig().getString(section + ".Permissions.Heal");
			HealOthersPerm = plugin.getConfig().getString(section + ".Permissions.HealOthers");
			FeedPerm = plugin.getConfig().getString(section + ".Permissions.Feed");
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player p = null;

		if (sender instanceof Player) {
			p = (Player) sender;
		}

		if (sender instanceof ConsoleCommandSender) {
			if (args.length > 0) {
				String possibleName = args[args.length - 1];
				if (Bukkit.getPlayer(possibleName) != null) {
					p = Bukkit.getPlayer(possibleName);
				} else {
					if (args.length == 2) {
						Util.consoleMSG("&fUsage: &c/" + label + " <player>");
					}
					return true;
				}
			} else {
				if (!label.equalsIgnoreCase("healall")) {
					Util.consoleMSG("&fUsage: &c/" + label + " <player>");
					return true;
				}
			}
		}

		if (label.equalsIgnoreCase("feed")) {
			if (checkPerm(sender, cmd.getName(), FeedPerm)) {
				Util.coloredMessage(p, configUtils.lang("HEAL_FED"));
				p.setFoodLevel(20);
				p.setSaturation(2);
			}
		} else if (label.equalsIgnoreCase("heal")) {
			if (checkPerm(sender, cmd.getName(), HealPerm)) {
				heal(p);
			}
		} else if (label.equalsIgnoreCase("healall")) {
			if (checkPerm(sender, cmd.getName(), HealOthersPerm)) {
				Bukkit.getOnlinePlayers().forEach(target -> Schedulers.entity(plugin, target, () -> heal(target)));
			}
		}

		return true;
	}

	public void heal(Player p) {
		p.setHealth(p.getMaxHealth());
		p.setFoodLevel(20);
		Util.coloredMessage(p, configUtils.lang("HEAL_HEALED"));
	}

	public boolean checkPerm(CommandSender p, String CMD, String perm) {
		if (!p.hasPermission(perm)) {
			Util.coloredMessage(p, "&cYou do not have access to &n/" + CMD + "&c.");
			return false;
		}
		return true;
	}

}
