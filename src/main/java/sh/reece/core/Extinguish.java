package sh.reece.core;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Extinguish extends BaseCommand {

	public static final String NOT_ONLINE = "<red>Player <u><player></u> is not online.";
	public static final String EXTINGUISHED = "<green>[+] You have been extinguished!";
	public static final String EXTINGUISHED_OTHER = "<green>[+] Successfully Extinguished <u><player>";

	private String StaffPermission;

	public Extinguish(Main instance) {
		super(instance, "Core.Extinguish", "extinguish");
		if (isEnabled()) {
			StaffPermission = plugin.getConfig().getString(section + ".StaffPermission");
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (noPermission(sender, cmd)) return true;

		Player target = (Player) sender;

		// if player tries to ext someone else & is a staff
		if (args.length > 0 && target.hasPermission(StaffPermission)) {
			if (Bukkit.getPlayer(args[0]) != null) {
				target = Bukkit.getPlayer(args[0]);
			} else {
				sender.sendRichMessage(NOT_ONLINE, Placeholder.unparsed("player", args[0]));
				return true;
			}
		}

		// remove fire from the player
		target.setFireTicks(0);
		target.sendRichMessage(EXTINGUISHED);

		if (!target.getName().equalsIgnoreCase(sender.getName())) {
			sender.sendRichMessage(EXTINGUISHED_OTHER, Placeholder.unparsed("player", target.getName()));
		}

		return true;
	}
}
