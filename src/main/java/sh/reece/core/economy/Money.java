package sh.reece.core.economy;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.OptionalLong;

/**
 * Money helpers. Internally every balance is whole cents held in a {@code long}
 * so we never touch a float until Vault forces us to at the API boundary.
 * That kills the usual under/overflow and "$0.1 + $0.2" rounding bugs.
 */
public final class Money {

	private Money() {}

	// hard ceiling so a single account can't overflow a long. 1 trillion dollars.
	public static final long MAX_CENTS = 100_000_000_000_000L;

	/**
	 * Parse user input like "10", "10.5", "1,000.99", "$5" into cents.
	 * Empty result means invalid: negative, non-numeric, or more precise than cents.
	 */
	public static OptionalLong parse(String input) {
		if (input == null) {
			return OptionalLong.empty();
		}
		String s = input.trim().replace(",", "").replace("$", "");
		if (s.isEmpty() || s.startsWith("-")) {
			return OptionalLong.empty(); // no negatives, including "-0.00"
		}
		try {
			BigDecimal d = new BigDecimal(s);
			if (d.scale() < 0 || d.scale() > 2) {
				// scale < 0 is exponent notation (1e3); scale > 2 is finer than a cent (10.999)
				return OptionalLong.empty();
			}
			if (d.signum() < 0) {
				return OptionalLong.empty();
			}
			long cents = d.movePointRight(2).longValueExact();
			if (cents > MAX_CENTS) {
				return OptionalLong.empty();
			}
			return OptionalLong.of(cents);
		} catch (ArithmeticException | NumberFormatException e) {
			return OptionalLong.empty();
		}
	}

	/** Vault hands us doubles; round to the nearest cent on the way in. */
	public static long toCents(double dollars) {
		return Math.round(dollars * 100.0);
	}

	/** Only used at the Vault API boundary where a double is required. */
	public static double toDollars(long cents) {
		return cents / 100.0;
	}

	// DecimalFormat isn't thread-safe and allocating one per call is wasteful on a
	// hot path (balance lookups, baltop). One instance per thread dodges both.
	private static final ThreadLocal<DecimalFormat> FMT =
		ThreadLocal.withInitial(() -> new DecimalFormat("#,##0.00"));

	/** e.g. (105099, "$") -> "$1,050.99" */
	public static String format(long cents, String symbol) {
		return symbol + FMT.get().format(toDollars(cents));
	}
}
