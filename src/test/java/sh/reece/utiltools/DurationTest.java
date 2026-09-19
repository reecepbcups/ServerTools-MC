package sh.reece.utiltools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Covers the cooldown duration strings used by config values (Cooldown: 15m)
 * and permission nodes (servertools.repair.all.cooldown.15m).
 */
class DurationTest {

	@Test
	void parsesBareSeconds() {
		assertEquals(600, TimeUtil.parseDuration("600"));
		assertEquals(0, TimeUtil.parseDuration("0"));
	}

	@Test
	void parsesSingleUnits() {
		assertEquals(600, TimeUtil.parseDuration("600s"));
		assertEquals(900, TimeUtil.parseDuration("15m"));
		assertEquals(3600, TimeUtil.parseDuration("1h"));
		assertEquals(86400, TimeUtil.parseDuration("1d"));
	}

	@Test
	void parsesCombinedUnitsAndIgnoresCase() {
		assertEquals(5400, TimeUtil.parseDuration("1h30m"));
		assertEquals(90, TimeUtil.parseDuration("1M30S"));
		assertEquals(900, TimeUtil.parseDuration("  15m "));
	}

	@Test
	void returnsNegativeOneForJunk() {
		assertEquals(-1, TimeUtil.parseDuration(null));
		assertEquals(-1, TimeUtil.parseDuration(""));
		assertEquals(-1, TimeUtil.parseDuration("fifteen"));
		assertEquals(-1, TimeUtil.parseDuration("15x"));
		assertEquals(-1, TimeUtil.parseDuration("15m junk"));
	}

	@Test
	void formatsSecondsBackToShortString() {
		assertEquals("0s", TimeUtil.formatDuration(0));
		assertEquals("0s", TimeUtil.formatDuration(-5));
		assertEquals("30s", TimeUtil.formatDuration(30));
		assertEquals("15m 30s", TimeUtil.formatDuration(930));
		assertEquals("15m", TimeUtil.formatDuration(900));
		assertEquals("1h 30m", TimeUtil.formatDuration(5400));
	}
}
