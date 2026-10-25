package sh.reece.core;

import sh.reece.core.teleport.TeleportAction;
import sh.reece.core.teleport.Teleporter;
import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Schedulers;
import sh.reece.utiltools.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class TP extends BaseCommand {

	private String TPPerm, TPAPerm, TPHerePerm, TPAHerePerm;
	private long expireMillis;
	private TeleportAction tpaAction;

	// here = the receiver moves to the sender (/tpahere)
	private record Request(UUID from, boolean here) {}

	// concurrent since Folia runs each player's commands on their own region thread
	private final Map<UUID, Request> currentRequest = new ConcurrentHashMap<>();

	public TP(Main instance) {
		super(instance, "Core.Teleport", "teleport", "tp", "tpo", "tpa", "tpahere", "tphere", "tpaccept", "tpdeny", "tpyes", "tpno", "tpcancel");
		if (isEnabled()) {
			TPPerm = plugin.getConfig().getString(section + ".Permissions.TP");
			TPAPerm = plugin.getConfig().getString(section + ".Permissions.TPA");
			TPHerePerm = plugin.getConfig().getString(section + ".Permissions.TPHere");
			TPAHerePerm = plugin.getConfig().getString(section + ".Permissions.TPAHere", "tools.tpahere");
			expireMillis = Math.max(1, plugin.getConfig().getInt(section + ".RequestExpireSeconds", 60)) * 1000L;
			tpaAction = TeleportAction.fromConfig(plugin.getConfig(), section);
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		// Console can only use tp/tpo
		if (!(sender instanceof Player)) {
			String lbl = label.toLowerCase();
			if (!lbl.equals("tp") && !lbl.equals("tpo")) {
				sender.sendMessage("This command can only be used by players.");
				return true;
			}
			return handleConsoleTp(sender, args);
		}

		Player p = (Player) sender;

		switch (label.toLowerCase()) {
		case "tpaccept":
		case "tpyes":
			Request accepted = currentRequest.remove(p.getUniqueId());
			if (accepted != null) {
				Player from = Bukkit.getPlayer(accepted.from());
				if (from != null) {
					Util.coloredMessage(from, configUtils.lang("TELEPORT_HASACCEPTED").replace("%player%", p.getName()));
					if (accepted.here()) {
						move(p, from, from);
					} else {
						move(from, p, from);
					}
				}
				Util.coloredMessage(p, configUtils.lang("TELEPORT_ACCEPTED").replace("%player%", from != null ? from.getName() : "?"));

			} else {
				Util.coloredMessage(p, configUtils.lang("TELEPORT_NOREQUEST"));
			}
			return true;

		case "tpcancel":
		case "tpdeny":
		case "tpno":
			Request denied = currentRequest.remove(p.getUniqueId());
			if (denied != null) {
				Player from = Bukkit.getPlayer(denied.from());
				if (from != null) {
					Util.coloredMessage(from, configUtils.lang("TELEPORT_HASDENIED").replace("%player%", p.getName()));
				}
				Util.coloredMessage(p, configUtils.lang("TELEPORT_DENIED").replace("%player%", from != null ? from.getName() : "?"));
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
						p.teleportAsync(target.getLocation());
					} else {
						return true;
					}
					break;

				case "tphere":
					if (checkPerm(p, label, TPHerePerm)) {
						output = "&aTeleported &f" + args[0] + " &ato &fYou";
						target.teleportAsync(p.getLocation());
						Util.coloredMessage(target, configUtils.lang("TELEPORT_TO").replace("%player%", p.getName()));
					} else {
						return true;
					}
					break;

				case "tpa":
					if (checkPerm(p, label, TPAPerm)) {
						sendRequest(p, target, false);
						return true;
					}
					break;

				case "tpahere":
					if (checkPerm(p, label, TPAHerePerm)) {
						sendRequest(p, target, true);
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

	// tp <player> <target> OR tp <player> <x> <y> <z>
	private boolean handleConsoleTp(CommandSender sender, String[] args) {
		if (args.length < 2) {
			sender.sendMessage("Usage: tp <player> <target> OR tp <player> <x> <y> <z>");
			return true;
		}

		Player target = Bukkit.getPlayer(args[0]);
		if (target == null) {
			sender.sendMessage("Player " + args[0] + " is not online.");
			return true;
		}

		// tp <player> <x> <y> <z>
		if (args.length >= 4) {
			try {
				double x = Double.parseDouble(args[1]);
				double y = Double.parseDouble(args[2]);
				double z = Double.parseDouble(args[3]);
				target.teleportAsync(new Location(target.getWorld(), x, y, z));
				sender.sendMessage("Teleported " + target.getName() + " to " + x + ", " + y + ", " + z);
			} catch (NumberFormatException e) {
				sender.sendMessage("Invalid coordinates.");
			}
			return true;
		}

		// tp <player> <target>
		Player dest = Bukkit.getPlayer(args[1]);
		if (dest == null) {
			sender.sendMessage("Player " + args[1] + " is not online.");
			return true;
		}
		target.teleportAsync(dest.getLocation());
		sender.sendMessage("Teleported " + target.getName() + " to " + dest.getName());
		return true;
	}

	private void sendRequest(Player from, Player to, boolean here) {
		// the sender pays, so check cooldown and balance before bothering the other player
		Teleporter teleporter = Teleporter.get();
		if (teleporter != null && !teleporter.canStart(from, tpaAction)) {
			return;
		}

		Util.coloredMessage(from, configUtils.lang("TELEPORT_SENT_REQUEST").replace("%player%", to.getName()));

		Util.coloredMessage(to, configUtils.lang(here ? "TELEPORT_GOT_HERE_REQUEST1" : "TELEPORT_GOT_REQUEST1").replace("%player%", from.getName()));
		Util.coloredMessage(to, configUtils.lang("TELEPORT_GOT_REQUEST2").replace("%player%", from.getName()));
		Request request = new Request(from.getUniqueId(), here);
		currentRequest.put(to.getUniqueId(), request);

		// only expire this exact request, a newer one replaced it otherwise
		Schedulers.asyncLater(plugin, () -> {
			if (currentRequest.remove(to.getUniqueId(), request)) {
				Player sender = Bukkit.getPlayer(request.from());
				if (sender != null) {
					Util.coloredMessage(sender, configUtils.lang("TELEPORT_TIMEOUT"));
				}
			}
		}, expireMillis);
	}

	// mover goes to destination's location once the warmup ends, the requester pays
	private void move(Player mover, Player destination, Player payer) {
		Teleporter teleporter = Teleporter.get();
		if (teleporter == null) {
			mover.teleportAsync(destination.getLocation());
			return;
		}
		teleporter.teleport(mover, payer, tpaAction,
			() -> CompletableFuture.completedFuture(destination.isOnline() ? destination.getLocation() : null),
			destination.getName());
	}

	public boolean killRequest(Player p) {
		if (currentRequest.containsKey(p.getUniqueId())) {
			Player loser = Bukkit.getPlayer(currentRequest.get(p.getUniqueId()).from());
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
