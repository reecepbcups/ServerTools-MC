package sh.reece.core.economy;

import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;

/**
 * The Vault economy provider. This is what replaces EssentialsX: it registers with
 * Vault's ServicesManager so /pay, PlaceholderAPI's %vault_eco_*% placeholders, shops,
 * and every other Vault consumer read from us.
 *
 * All balances live in {@link EconomyStorage} as whole cents. We only convert to the
 * double that Vault demands at these method boundaries. Everything is keyed by UUID so
 * offline players and name changes work; the deprecated name-based methods resolve a
 * UUID through Bukkit first.
 */
public class ServerToolsEconomy extends AbstractEconomy {

	private final EconomyStorage storage;
	private final String symbol;
	private final String singular;
	private final String plural;

	public ServerToolsEconomy(EconomyStorage storage, String symbol, String singular, String plural) {
		this.storage = storage;
		this.symbol = symbol;
		this.singular = singular;
		this.plural = plural;
	}

	public EconomyStorage getStorage() {
		return storage;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public String getName() {
		return "ServerTools";
	}

	@Override
	public boolean hasBankSupport() {
		return false;
	}

	@Override
	public int fractionalDigits() {
		return 2;
	}

	@Override
	public String format(double amount) {
		return Money.format(Money.toCents(amount), symbol);
	}

	@Override
	public String currencyNamePlural() {
		return plural;
	}

	@Override
	public String currencyNameSingular() {
		return singular;
	}

	// account lookups (UUID-routed) ------------------------------------------

	@Override
	public boolean hasAccount(OfflinePlayer player) {
		return storage.has(player.getUniqueId());
	}

	@Override
	public boolean hasAccount(OfflinePlayer player, String world) {
		return hasAccount(player);
	}

	@Override
	public boolean createPlayerAccount(OfflinePlayer player) {
		return storage.createAccount(player.getUniqueId(), player.getName());
	}

	@Override
	public boolean createPlayerAccount(OfflinePlayer player, String world) {
		return createPlayerAccount(player);
	}

	@Override
	public double getBalance(OfflinePlayer player) {
		return Money.toDollars(storage.getCents(player.getUniqueId()));
	}

	@Override
	public double getBalance(OfflinePlayer player, String world) {
		return getBalance(player);
	}

	@Override
	public boolean has(OfflinePlayer player, double amount) {
		return storage.getCents(player.getUniqueId()) >= Money.toCents(amount);
	}

	@Override
	public boolean has(OfflinePlayer player, String world, double amount) {
		return has(player, amount);
	}

	@Override
	public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
		long cents = Money.toCents(amount);
		EconomyStorage.Result r = storage.withdraw(player.getUniqueId(), player.getName(), cents);
		return respond(r, player);
	}

	@Override
	public EconomyResponse withdrawPlayer(OfflinePlayer player, String world, double amount) {
		return withdrawPlayer(player, amount);
	}

	@Override
	public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
		long cents = Money.toCents(amount);
		EconomyStorage.Result r = storage.deposit(player.getUniqueId(), player.getName(), cents);
		return respond(r, player);
	}

	@Override
	public EconomyResponse depositPlayer(OfflinePlayer player, String world, double amount) {
		return depositPlayer(player, amount);
	}

	private EconomyResponse respond(EconomyStorage.Result r, OfflinePlayer player) {
		double bal = getBalance(player);
		switch (r) {
			case SUCCESS:
				return new EconomyResponse(0, bal, ResponseType.SUCCESS, null);
			case INSUFFICIENT_FUNDS:
				return new EconomyResponse(0, bal, ResponseType.FAILURE, "Insufficient funds");
			case OVERFLOW:
				return new EconomyResponse(0, bal, ResponseType.FAILURE, "Balance limit reached");
			default:
				return new EconomyResponse(0, bal, ResponseType.FAILURE, "Invalid amount");
		}
	}

	// deprecated name-based methods - resolve a UUID, then delegate ----------

	@Override
	public boolean hasAccount(String playerName) {
		return hasAccount(Bukkit.getOfflinePlayer(playerName));
	}

	@Override
	public boolean hasAccount(String playerName, String world) {
		return hasAccount(playerName);
	}

	@Override
	public boolean createPlayerAccount(String playerName) {
		return createPlayerAccount(Bukkit.getOfflinePlayer(playerName));
	}

	@Override
	public boolean createPlayerAccount(String playerName, String world) {
		return createPlayerAccount(playerName);
	}

	@Override
	public double getBalance(String playerName) {
		return getBalance(Bukkit.getOfflinePlayer(playerName));
	}

	@Override
	public double getBalance(String playerName, String world) {
		return getBalance(playerName);
	}

	@Override
	public boolean has(String playerName, double amount) {
		return has(Bukkit.getOfflinePlayer(playerName), amount);
	}

	@Override
	public boolean has(String playerName, String world, double amount) {
		return has(playerName, amount);
	}

	@Override
	public EconomyResponse withdrawPlayer(String playerName, double amount) {
		return withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount);
	}

	@Override
	public EconomyResponse withdrawPlayer(String playerName, String world, double amount) {
		return withdrawPlayer(playerName, amount);
	}

	@Override
	public EconomyResponse depositPlayer(String playerName, double amount) {
		return depositPlayer(Bukkit.getOfflinePlayer(playerName), amount);
	}

	@Override
	public EconomyResponse depositPlayer(String playerName, String world, double amount) {
		return depositPlayer(playerName, amount);
	}

	// no bank support ---------------------------------------------------------

	@Override
	public EconomyResponse createBank(String name, String player) {
		return unsupported();
	}

	@Override
	public EconomyResponse deleteBank(String name) {
		return unsupported();
	}

	@Override
	public EconomyResponse bankBalance(String name) {
		return unsupported();
	}

	@Override
	public EconomyResponse bankHas(String name, double amount) {
		return unsupported();
	}

	@Override
	public EconomyResponse bankWithdraw(String name, double amount) {
		return unsupported();
	}

	@Override
	public EconomyResponse bankDeposit(String name, double amount) {
		return unsupported();
	}

	@Override
	public EconomyResponse isBankOwner(String name, String playerName) {
		return unsupported();
	}

	@Override
	public EconomyResponse isBankMember(String name, String playerName) {
		return unsupported();
	}

	@Override
	public List<String> getBanks() {
		return Collections.emptyList();
	}

	private EconomyResponse unsupported() {
		return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "ServerTools has no bank support");
	}
}
