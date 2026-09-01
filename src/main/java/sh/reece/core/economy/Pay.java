package sh.reece.core.economy;

import java.util.OptionalLong;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

/**
 * /pay {@literal <player> <amount>} - move money between players. Talks to
 * {@link EconomyStorage} directly so the transfer is atomic and cent-accurate,
 * instead of round-tripping through Vault's double API.
 */
public class Pay extends BaseCommand {

	public Pay(Main instance) {
		super(instance, "Economy.Pay", "pay");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			Util.coloredMessage(sender, EcoFormat.msg(plugin, "Messages.PlayersOnly", "&c[!] Only players can pay."));
			return true;
		}
		EconomyStorage storage = plugin.getEconomyStorage();
		if (storage == null) {
			Util.coloredMessage(sender, EcoFormat.msg(plugin, "Messages.NotEnabled", "&c[!] The economy is not enabled."));
			return true;
		}
		if (noPermission(sender, cmd)) {
			return true;
		}
		if (args.length != 2) {
			Util.coloredMessage(sender, EcoFormat.msg(plugin, "Messages.PayUsage", "&e/pay <player> <amount>"));
			return true;
		}

		Player from = (Player) sender;
		String sym = plugin.getCurrencySymbol();

		OfflinePlayer to = resolvePayee(args[0]);
		if (to == null) {
			Util.coloredMessage(from, EcoFormat.msg(plugin, "Messages.NeverJoined",
				"&c[!] &f%player% &chas never joined the server.", "player", args[0]));
			return true;
		}
		if (to.getUniqueId().equals(from.getUniqueId())) {
			Util.coloredMessage(from, EcoFormat.msg(plugin, "Messages.PaySelf", "&c[!] You can't pay yourself."));
			return true;
		}

		OptionalLong parsed = Money.parse(args[1]);
		if (parsed.isEmpty()) {
			Util.coloredMessage(from, EcoFormat.msg(plugin, "Messages.InvalidAmount",
				"&c[!] &f%input% &cis not a valid amount.", "input", args[1]));
			return true;
		}
		long cents = parsed.getAsLong();

		EconomyStorage.Result r = storage.transfer(
			from.getUniqueId(), from.getName(),
			to.getUniqueId(), to.getName(),
			cents);

		switch (r) {
			case SUCCESS:
				String amt = Money.format(cents, sym);
				Util.coloredMessage(from, EcoFormat.msg(plugin, "Messages.PaySent",
					"&aYou paid &f%player% %amount%&a. New balance: &f%balance%",
					"player", to.getName(), "amount", amt,
					"balance", Money.format(storage.getCents(from.getUniqueId()), sym)));
				Player online = to.getPlayer();
				if (online != null) {
					Util.coloredMessage(online, EcoFormat.msg(plugin, "Messages.PayReceived",
						"&aYou received %amount% &afrom &f%player%&a. New balance: &f%balance%",
						"player", from.getName(), "amount", amt,
						"balance", Money.format(storage.getCents(to.getUniqueId()), sym)));
				}
				return true;
			case INSUFFICIENT_FUNDS:
				Util.coloredMessage(from, EcoFormat.msg(plugin, "Messages.PayInsufficient",
					"&c[!] You can't afford that. Balance: &f%balance%",
					"balance", Money.format(storage.getCents(from.getUniqueId()), sym)));
				return true;
			case OVERFLOW:
				Util.coloredMessage(from, EcoFormat.msg(plugin, "Messages.PayOverflow",
					"&c[!] That would put them over the balance limit."));
				return true;
			case INVALID_AMOUNT:
			default:
				Util.coloredMessage(from, EcoFormat.msg(plugin, "Messages.PayInvalidAmount",
					"&c[!] Enter an amount greater than 0."));
				return true;
		}
	}

	/** Online player, or a known offline player who has joined before. null if neither. */
	private OfflinePlayer resolvePayee(String name) {
		Player online = Bukkit.getPlayerExact(name);
		if (online != null) {
			return online;
		}
		OfflinePlayer off = Bukkit.getOfflinePlayer(name);
		if (off.hasPlayedBefore() || plugin.getEconomyStorage().has(off.getUniqueId())) {
			return off;
		}
		return null;
	}
}
