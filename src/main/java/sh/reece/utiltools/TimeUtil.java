package sh.reece.utiltools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Duration strings used by cooldown config values and cooldown permission nodes. */
public final class TimeUtil {

	private TimeUtil() {}

	private static final Pattern DURATION = Pattern.compile("(\\d+)([smhd]?)");

	/**
	 * Parses a duration like 600s, 15m, 1h30m or a bare number of seconds.
	 * Returns -1 when the value cannot be parsed so callers can tell it apart from "no cooldown".
	 */
	public static int parseDuration(final String value) {
		if (value == null) return -1;

		final String trimmed = value.trim().toLowerCase();
		if (trimmed.isEmpty()) return -1;

		final Matcher m = DURATION.matcher(trimmed);
		int seconds = 0;
		int consumed = 0;
		while (m.find()) {
			if (m.start() != consumed) return -1; // junk between the parts
			consumed = m.end();

			final long amount = Long.parseLong(m.group(1));
			switch (m.group(2)) {
				case "d": seconds += amount * 86400; break;
				case "h": seconds += amount * 3600; break;
				case "m": seconds += amount * 60; break;
				default: seconds += amount; break;
			}
		}

		return consumed == trimmed.length() ? seconds : -1;
	}

	/** Formats seconds back into a short human string, e.g. 930 -> "15m 30s". */
	public static String formatDuration(final long totalSeconds) {
		if (totalSeconds <= 0) return "0s";

		final long hours = totalSeconds / 3600;
		final long minutes = (totalSeconds % 3600) / 60;
		final long seconds = totalSeconds % 60;

		final StringBuilder sb = new StringBuilder();
		if (hours > 0) sb.append(hours).append("h ");
		if (minutes > 0) sb.append(minutes).append("m ");
		if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");

		return sb.toString().trim();
	}
}
