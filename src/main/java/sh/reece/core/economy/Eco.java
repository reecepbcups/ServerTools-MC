package sh.reece.core.economy;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

/**
 * /eco {@literal <give|take|set> <player> <amount>} - admin money management,
 * the equivalent of Essentials' /eco. Guarded by the Economy.Eco permission.
 */
public class Eco extends BaseCommand {

	public Eco(Main instance) {
		super(instance, "Economy.Admin", "eco");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		EconomyStorage storage = plugin.getEconomyStorage();
		if (storage == null) {
			Util.coloredMessage(sender, EcoFormat.msg(plugin, "Messages.NotEnabled", "&c[!] The economy is not enabled."));
			return true;
		}
		if (noPermission(sender, cmd)) {
			return true;
		}
		if (args.length != 3) {
			Util.coloredMessage(sender, EcoFormat.msg(plugin, "Messages.EcoUsage", "&e/eco <give|take|set> <player> <amount>"));
			return true;
		}

		String action = args[0].toLowerCase();
		String sym = plugin.getCurrencySymbol();

		OfflinePlayer target = Bukkit.getPlayerExact(args[1]);
		if (target == null) {
			OfflinePlayer off = Bukkit.getOfflinePlayer(args[1]);
			target = (off.hasPlayedBefore() || storage.has(off.getUniqueId())) ? off : null;
		}
		if (target == null) {
			Util.coloredMessage(sender, EcoFormat.msg(plugin, "Messages.NeverJoined",
				"&c[!] &f%player% &chas never joined the server.", "player", args[1]));
			return true;
		}

		OptionalLong parsed = Money.parse(args[2]);
		if (parsed.isEmpty()) {
			Util.coloredMessage(sender, EcoFormat.msg(plugin, "Messages.InvalidAmount",
				"&c[!] &f%input% &cis not a valid amount.", "input", args[2]));
			return true;
		}
		long cents = parsed.getAsLong();

		EconomyStorage.Result r;
		switch (action) {
			case "give":
			case "add":
				r = storage.deposit(target.getUniqueId(), target.getName(), cents);
				break;
			case "take":
			case "remove":
				r = storage.withdraw(target.getUniqueId(), target.getName(), cents);
				break;
			case "set":
				r = storage.set(target.getUniqueId(), target.getName(), cents);
				break;
			default:
				Util.coloredMessage(sender, EcoFormat.msg(plugin, "Messages.EcoUsage", "&e/eco <give|take|set> <player> <amount>"));
				return true;
		}

		String bal = Money.format(storage.getCents(target.getUniqueId()), sym);
		switch (r) {
			case SUCCESS:
				Util.coloredMessage(sender, EcoFormat.msg(plugin, "Messages.EcoSet",
					"&aSet &f%player%&a's balance. Now: &f%balance%",
					"player", target.getName(), "balance", bal));
				return true;
			case INSUFFICIENT_FUNDS:
				Util.coloredMessage(sender, EcoFormat.msg(plugin, "Messages.EcoInsufficient",
					"&c[!] &f%player% &conly has &f%balance%&c.",
					"player", target.getName(), "balance", bal));
				return true;
			case OVERFLOW:
				Util.coloredMessage(sender, EcoFormat.msg(plugin, "Messages.EcoOverflow",
					"&c[!] That exceeds the balance limit."));
				return true;
			case INVALID_AMOUNT:
			default:
				Util.coloredMessage(sender, EcoFormat.msg(plugin, "Messages.PayInvalidAmount",
					"&c[!] Invalid amount."));
				return true;
		}
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
		if (args.length == 1) {
			String p = args[0].toLowerCase();
			return Arrays.asList("give", "take", "set").stream()
				.filter(a -> a.startsWith(p)).collect(Collectors.toList());
		}
		if (args.length == 2) {
			String p = args[1].toLowerCase();
			return Bukkit.getOnlinePlayers().stream()
				.map(pl -> pl.getName())
				.filter(n -> n.toLowerCase().startsWith(p))
				.collect(Collectors.toList());
		}
		return List.of();
	}
}
