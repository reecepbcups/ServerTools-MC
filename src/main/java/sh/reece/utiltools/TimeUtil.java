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

	/**
	 * Minecraft time of day for /time and /ptime: day, noon, sunset, night, midnight, sunrise,
	 * ticks (6000 or 6000t) or a clock time (14:30). Returns -1 when it can't be read.
	 */
	public static long parseWorldTime(final String value) {
		if (value == null) return -1;

		final String v = value.trim().toLowerCase();
		switch (v) {
			case "day": case "morning": return 1000;
			case "noon": return 6000;
			case "sunset": case "dusk": case "evening": return 12000;
			case "night": return 13000;
			case "midnight": return 18000;
			case "sunrise": case "dawn": return 23000;
			default: break;
		}

		final int colon = v.indexOf(':');
		try {
			if (colon > 0) {
				final int hours = Integer.parseInt(v.substring(0, colon));
				final int minutes = Integer.parseInt(v.substring(colon + 1));
				if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) return -1;
				// tick 0 is 06:00
				return ((hours + 18) % 24) * 1000L + minutes * 1000L / 60;
			}
			final long ticks = Long.parseLong(v.endsWith("t") ? v.substring(0, v.length() - 1) : v);
			return ticks < 0 ? -1 : ticks % 24000;
		} catch (final NumberFormatException e) {
			return -1;
		}
	}

	/** World ticks as a clock, e.g. 6000 -> "12:00". */
	public static String formatWorldTime(final long ticks) {
		final long dayTicks = ((ticks % 24000) + 24000) % 24000;
		final long hours = (dayTicks / 1000 + 6) % 24;
		final long minutes = (dayTicks % 1000) * 60 / 1000;
		return String.format("%02d:%02d", hours, minutes);
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
