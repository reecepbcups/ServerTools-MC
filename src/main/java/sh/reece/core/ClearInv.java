package sh.reece.core;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.TextUtil;

public class ClearInv extends BaseCommand {

	public static final String TARGET_NOT_ONLINE = "<white>[<red>!<white>] <red>Target <player> is not online.";
	public static final String CLEARED_OTHER = "<white>[<red>!<white>] <yellow>Cleared <player><yellow> inventory";
	public static final String CLEARED = "<gray>[<red>!<gray>] <white>Your inventory has been cleared.";

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
				TextUtil.consoleMessage("<white>Usage: /" + cmd.getName() + " \\<player>");
			}

		} else if (args.length == 1) {
			if (hasPermission(sender, permission.isEmpty() ? "" : permission + ".others")) {
				Player target = Bukkit.getPlayer(args[0]);

				if (target == null) {
					// player input, so it is passed unparsed rather than concatenated into the
					// format string where its tags would be read as markup
					sender.sendRichMessage(TARGET_NOT_ONLINE, Placeholder.unparsed("player", args[0]));
				} else {
					clearinv(target);
					sender.sendRichMessage(CLEARED_OTHER, Placeholder.component("player", target.displayName()));
				}
			}
		}
		return true;
	}

	public void clearinv(Player p) {
		p.getInventory().clear();
		p.sendRichMessage(CLEARED);
	}

}
