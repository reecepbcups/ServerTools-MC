package sh.reece.core.economy;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

/**
 * /balance [player] and /baltop. Reads straight from {@link EconomyStorage}.
 * Both commands share this executor; we branch on the label.
 */
public class Balance extends BaseCommand {

	public Balance(Main instance) {
		super(instance, "Economy.Balance", "balance", "baltop");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		EconomyStorage storage = plugin.getEconomyStorage();
		if (storage == null) {
			Util.coloredMessage(sender, EcoFormat.msg(plugin, "Messages.NotEnabled", "&c[!] The economy is not enabled."));
			return true;
		}
		String sym = plugin.getCurrencySymbol();

		if (cmd.getName().equalsIgnoreCase("baltop")) {
			return baltop(sender, storage, sym);
		}

		// /balance [player]
		OfflinePlayer target;
		if (args.length >= 1) {
			target = Bukkit.getPlayerExact(args[0]);
			if (target == null) {
				OfflinePlayer off = Bukkit.getOfflinePlayer(args[0]);
				target = (off.hasPlayedBefore() || storage.has(off.getUniqueId())) ? off : null;
			}
			if (target == null) {
				Util.coloredMessage(sender, EcoFormat.msg(plugin, "Messages.NeverJoined",
					"&c[!] &f%player% &chas never joined the server.", "player", args[0]));
				return true;
			}
		} else if (sender instanceof Player) {
			target = (Player) sender;
		} else {
			Util.coloredMessage(sender, EcoFormat.msg(plugin, "Messages.BalanceUsage", "&e/balance <player>"));
			return true;
		}

		String bal = Money.format(storage.getCents(target.getUniqueId()), sym);
		if (sender instanceof Player && target.getUniqueId().equals(((Player) sender).getUniqueId())) {
			Util.coloredMessage(sender, EcoFormat.msg(plugin, "Messages.BalanceSelf",
				"&aBalance: &f%balance%", "balance", bal));
		} else {
			Util.coloredMessage(sender, EcoFormat.msg(plugin, "Messages.BalanceOther",
				"&f%player%&a's balance: &f%balance%", "player", target.getName(), "balance", bal));
		}
		return true;
	}

	private boolean baltop(CommandSender sender, EconomyStorage storage, String sym) {
		int limit = plugin.getConfig().getInt("Economy.BalTop.Limit", 10);
		Map<java.util.UUID, Long> top = storage.top(limit);
		Util.coloredMessage(sender, EcoFormat.msg(plugin, "BalTop.Header", "&e&lTop Balances"));
		if (top.isEmpty()) {
			Util.coloredMessage(sender, EcoFormat.msg(plugin, "BalTop.Empty", "&7No accounts yet."));
			return true;
		}
		int rank = 1;
		for (Map.Entry<java.util.UUID, Long> e : top.entrySet()) {
			String name = Bukkit.getOfflinePlayer(e.getKey()).getName();
			if (name == null) {
				name = e.getKey().toString();
			}
			Util.coloredMessage(sender, EcoFormat.msg(plugin, "BalTop.Line",
				"&e%rank%. &f%player% &7- &a%balance%",
				"rank", String.valueOf(rank), "player", name,
				"balance", Money.format(e.getValue(), sym)));
			rank++;
		}
		return true;
	}
}
