package sh.reece.utiltools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The time of day words and clock times /time and /ptime take. */
class WorldTimeTest {

	@Test
	void parsesNames() {
		assertEquals(1000, TimeUtil.parseWorldTime("day"));
		assertEquals(6000, TimeUtil.parseWorldTime("Noon"));
		assertEquals(13000, TimeUtil.parseWorldTime("night"));
		assertEquals(18000, TimeUtil.parseWorldTime("midnight"));
	}

	@Test
	void parsesTicks() {
		assertEquals(6000, TimeUtil.parseWorldTime("6000"));
		assertEquals(6000, TimeUtil.parseWorldTime("6000t"));
		assertEquals(1000, TimeUtil.parseWorldTime("25000"));
	}

	@Test
	void parsesClockTimes() {
		assertEquals(0, TimeUtil.parseWorldTime("6:00"));
		assertEquals(6000, TimeUtil.parseWorldTime("12:00"));
		assertEquals(18000, TimeUtil.parseWorldTime("00:00"));
		assertEquals(8500, TimeUtil.parseWorldTime("14:30"));
	}

	@Test
	void rejectsJunk() {
		assertEquals(-1, TimeUtil.parseWorldTime(null));
		assertEquals(-1, TimeUtil.parseWorldTime("lunch"));
		assertEquals(-1, TimeUtil.parseWorldTime("-5"));
		assertEquals(-1, TimeUtil.parseWorldTime("25:00"));
	}

	@Test
	void formatsAsClock() {
		assertEquals("06:00", TimeUtil.formatWorldTime(0));
		assertEquals("12:00", TimeUtil.formatWorldTime(6000));
		assertEquals("00:00", TimeUtil.formatWorldTime(18000));
		assertEquals("14:30", TimeUtil.formatWorldTime(8500));
	}
}
