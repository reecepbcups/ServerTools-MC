package sh.reece.core.economy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * SQLite-backed balance store. Everything here is whole cents (long) - no floats.
 * Reads come from an in-memory cache; mutations update the cache under a lock (so
 * transfers stay atomic) then queue the disk write onto a single background thread -
 * the main thread never blocks on SQLite IO. No Bukkit imports, so it unit tests
 * against an in-memory database (jdbc:sqlite::memory:).
 */
public class EconomyStorage {

	public enum Result {
		SUCCESS, INSUFFICIENT_FUNDS, INVALID_AMOUNT, OVERFLOW, SAME_ACCOUNT
	}

	private final String jdbcUrl;
	private final long startingCents;
	private Connection conn;
	private PreparedStatement upsertStmt;
	// single background thread drains queued writes in order, so the reusable upsert
	// statement is only ever touched off the main thread and writes stay serialized.
	private ExecutorService writer;
	private final Map<UUID, Long> cache = new ConcurrentHashMap<>();

	public EconomyStorage(String jdbcUrl, long startingCents) {
		this.jdbcUrl = jdbcUrl;
		this.startingCents = startingCents;
	}

	public void open() throws SQLException {
		conn = DriverManager.getConnection(jdbcUrl);
		try (PreparedStatement st = conn.prepareStatement(
				"CREATE TABLE IF NOT EXISTS balances (uuid TEXT PRIMARY KEY, cents INTEGER NOT NULL DEFAULT 0, name TEXT)")) {
			st.execute();
		}
		// warm the cache from disk so reads never block on IO
		try (PreparedStatement st = conn.prepareStatement("SELECT uuid, cents FROM balances");
				ResultSet rs = st.executeQuery()) {
			while (rs.next()) {
				cache.put(UUID.fromString(rs.getString("uuid")), rs.getLong("cents"));
			}
		}
		// one reusable statement for all writes - every mutation is synchronized, so
		// there's never concurrent access to it, and we skip re-preparing per write.
		upsertStmt = conn.prepareStatement(
			"INSERT INTO balances(uuid, cents, name) VALUES(?,?,?) "
			+ "ON CONFLICT(uuid) DO UPDATE SET cents=excluded.cents, name=excluded.name");
		writer = Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r, "ServerTools-EconomyWriter");
			t.setDaemon(true);
			return t;
		});
	}

	public void close() {
		// drain any queued writes before we tear down the connection, so a shutdown
		// right after a deposit still persists it.
		if (writer != null) {
			writer.shutdown();
			try {
				writer.awaitTermination(30, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		if (upsertStmt != null) {
			try {
				upsertStmt.close();
			} catch (SQLException ignored) {
			}
		}
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException ignored) {
			}
		}
	}

	public boolean has(UUID id) {
		return cache.containsKey(id);
	}

	public long getCents(UUID id) {
		return cache.getOrDefault(id, 0L);
	}

	/** Create the account at the starting balance if it doesn't exist. Returns true if created. */
	public synchronized boolean createAccount(UUID id, String name) {
		if (cache.containsKey(id)) {
			return false;
		}
		cache.put(id, startingCents);
		enqueueUpsert(id, name, startingCents);
		return true;
	}

	public synchronized Result deposit(UUID id, String name, long cents) {
		if (cents < 0) {
			return Result.INVALID_AMOUNT;
		}
		long cur = cache.getOrDefault(id, startingCents);
		if (cur > MAX_MINUS(cents)) {
			return Result.OVERFLOW;
		}
		put(id, name, cur + cents);
		return Result.SUCCESS;
	}

	public synchronized Result withdraw(UUID id, String name, long cents) {
		if (cents < 0) {
			return Result.INVALID_AMOUNT;
		}
		long cur = cache.getOrDefault(id, startingCents);
		if (cur < cents) {
			return Result.INSUFFICIENT_FUNDS;
		}
		put(id, name, cur - cents);
		return Result.SUCCESS;
	}

	public synchronized Result set(UUID id, String name, long cents) {
		if (cents < 0) {
			return Result.INVALID_AMOUNT;
		}
		if (cents > Money.MAX_CENTS) {
			return Result.OVERFLOW;
		}
		put(id, name, cents);
		return Result.SUCCESS;
	}

	/** Atomic move of cents from one account to another. */
	public synchronized Result transfer(UUID from, String fromName, UUID to, String toName, long cents) {
		if (cents <= 0) {
			return Result.INVALID_AMOUNT;
		}
		if (from.equals(to)) {
			return Result.SAME_ACCOUNT;
		}
		long fromBal = cache.getOrDefault(from, startingCents);
		if (fromBal < cents) {
			return Result.INSUFFICIENT_FUNDS;
		}
		long toBal = cache.getOrDefault(to, startingCents);
		if (toBal > MAX_MINUS(cents)) {
			return Result.OVERFLOW;
		}
		put(from, fromName, fromBal - cents);
		put(to, toName, toBal + cents);
		return Result.SUCCESS;
	}

	/** Highest balances first, capped at limit. */
	public Map<UUID, Long> top(int limit) {
		Map<UUID, Long> out = new LinkedHashMap<>();
		cache.entrySet().stream()
			.sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
			.limit(limit)
			.forEach(e -> out.put(e.getKey(), e.getValue()));
		return out;
	}

	private static long MAX_MINUS(long cents) {
		return Money.MAX_CENTS - cents;
	}

	private void put(UUID id, String name, long cents) {
		cache.put(id, cents);
		enqueueUpsert(id, name, cents);
	}

	// hand the write to the background thread; the cache already holds the truth,
	// so callers return immediately without waiting on disk.
	private void enqueueUpsert(UUID id, String name, long cents) {
		writer.execute(() -> upsert(id, name, cents));
	}

	private void upsert(UUID id, String name, long cents) {
		try {
			upsertStmt.setString(1, id.toString());
			upsertStmt.setLong(2, cents);
			upsertStmt.setString(3, name);
			upsertStmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("economy write failed for " + id, e);
		}
	}
}
