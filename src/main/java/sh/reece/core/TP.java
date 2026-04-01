package sh.reece.core;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TP extends BaseCommand {

	private String TPPerm, TPAPerm, TPHerePerm;

	private final Map<UUID, UUID> currentRequest = new HashMap<>();

	public TP(Main instance) {
		super(instance, "Core.Teleport", "teleport", "tp", "tpo", "tpa", "tphere", "tpaccept", "tpdeny", "tpyes", "tpno", "tpcancel");
		if (isEnabled()) {
			TPPerm = plugin.getConfig().getString(section + ".Permissions.TP");
			TPAPerm = plugin.getConfig().getString(section + ".Permissions.TPA");
			TPHerePerm = plugin.getConfig().getString(section + ".Permissions.TPHere");
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player p = (Player) sender;

		switch (label.toLowerCase()) {
		case "tpaccept":
		case "tpyes":
			if (currentRequest.containsKey(p.getUniqueId())) {
				Player from = Bukkit.getPlayer(currentRequest.get(p.getUniqueId()));
				if (from != null) {
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
			if (currentRequest.containsKey(p.getUniqueId())) {
				Player from = Bukkit.getPlayer(currentRequest.get(p.getUniqueId()));
				if (from != null) {
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
			Util.coloredMessage(p, "&fUsage: &c/" + label + " <player>");
		} else {
			Player target = Bukkit.getPlayer(args[0]);
			String output = "&cPlayer " + args[0] + " is not online.";

			if (target == p) {
				output = configUtils.lang("TELEPORT_SELF");
			} else if (target != null) {

				switch (label.toLowerCase()) {
				case "tp":
				case "tpo":
					if (checkPerm(p, label, TPPerm)) {
						output = configUtils.lang("TELEPORT_TO").replace("%player%", args[0]);
						p.teleport(target);
					} else {
						return true;
					}
					break;

				case "tphere":
					if (checkPerm(p, label, TPHerePerm)) {
						output = "&aTeleported &f" + args[0] + " &ato &fYou";
						target.teleport(p);
						Util.coloredMessage(target, configUtils.lang("TELEPORT_TO").replace("%player%", p.getName()));
					} else {
						return true;
					}
					break;

				case "tpa":
					if (checkPerm(p, label, TPAPerm)) {
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
			Util.coloredMessage(p, "&cYou do not have access to &n/" + CMD + "&c.");
			return false;
		}
		return true;
	}

}
