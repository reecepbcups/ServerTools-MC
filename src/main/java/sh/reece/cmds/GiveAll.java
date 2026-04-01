package sh.reece.cmds;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class GiveAll extends BaseCommand {

	public GiveAll(Main instance) {
        super(instance, "Commands.GiveAll", "giveall");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (noPermission(sender, cmd)) return true;

		if (args.length < 2) {
			sendHelpMenu(sender);
			return true;
		}

		Material mat = Material.getMaterial(args[0].toUpperCase());
		if(mat == null) {
			sender.sendMessage(Util.color("&cInvalid Material " + args[0] + ". Try using /itemdb to get the item name"));
			return true;
		}

		final Integer amount = Integer.parseInt(args[1]);

		Util.coloredMessage(sender, "&aGiving &6" + amount + " &aof &6" + mat.name() + "&a to all players");

		String consoleCMD = String.format("minecraft:give @a %s %s", mat.toString().toLowerCase(), amount.toString());
		Util.console(consoleCMD);
		Util.log(consoleCMD);
		return true;
	}

	public void sendHelpMenu(CommandSender s) {
		s.sendMessage("/giveall <item> <amount>");
	}
}
