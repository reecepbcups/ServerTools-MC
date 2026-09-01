package sh.reece.core.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalLong;

import org.junit.jupiter.api.Test;

class MoneyTest {

	private long cents(String in) {
		OptionalLong r = Money.parse(in);
		assertTrue(r.isPresent(), "expected parseable: " + in);
		return r.getAsLong();
	}

	@Test
	void parsesWholeDollars() {
		assertEquals(1000, cents("10"));
	}

	@Test
	void parsesCents() {
		assertEquals(1050, cents("10.5"));
		assertEquals(1050, cents("10.50"));
		assertEquals(1, cents("0.01"));
		assertEquals(0, cents("0"));
	}

	@Test
	void stripsCommasAndDollarSign() {
		assertEquals(100000, cents("1,000"));
		assertEquals(500, cents("$5"));
		assertEquals(123456, cents("$1,234.56"));
	}

	@Test
	void rejectsSubCentPrecision() {
		assertFalse(Money.parse("10.999").isPresent());
		assertFalse(Money.parse("0.001").isPresent());
	}

	@Test
	void zeroIsValidButExact() {
		assertEquals(0, cents("0"));
		assertEquals(0, cents("0.00"));
		assertEquals(0, cents("0.0"));
		assertEquals(0, cents("$0.00"));
	}

	@Test
	void rejectsNegatives() {
		assertFalse(Money.parse("-5").isPresent());
		assertFalse(Money.parse("-0.01").isPresent());
		assertFalse(Money.parse("-0.00").isPresent()); // negative zero -> reject to be safe
	}

	@Test
	void rejectsGarbageAndBadMath() {
		assertFalse(Money.parse("abc").isPresent());
		assertFalse(Money.parse("").isPresent());
		assertFalse(Money.parse("   ").isPresent());
		assertFalse(Money.parse(null).isPresent());
		assertFalse(Money.parse("1.2.3").isPresent());
		assertFalse(Money.parse("5+5").isPresent());   // arithmetic, not a number
		assertFalse(Money.parse("10*2").isPresent());
		assertFalse(Money.parse("10 5").isPresent());   // embedded space
		assertFalse(Money.parse("1_000").isPresent());  // underscore
		assertFalse(Money.parse("0x10").isPresent());   // hex
		assertFalse(Money.parse("NaN").isPresent());
		assertFalse(Money.parse("Infinity").isPresent());
	}

	@Test
	void trailingDotIsTreatedAsWholeDollars() {
		assertEquals(1000, cents("10.")); // BigDecimal reads "10." as 10; harmless
	}

	@Test
	void rejectsScientificNotation() {
		assertFalse(Money.parse("1e3").isPresent());
		assertFalse(Money.parse("1E3").isPresent());
		assertFalse(Money.parse("1.5e2").isPresent());
	}

	@Test
	void rejectsAboveMax() {
		assertFalse(Money.parse("999999999999999").isPresent());
	}

	@Test
	void doubleConversionRoundsToNearestCent() {
		assertEquals(1050, Money.toCents(10.50));
		assertEquals(1, Money.toCents(0.005)); // half rounds up
		assertEquals(0, Money.toCents(0.004));
		assertEquals(10.50, Money.toDollars(1050), 0.0000001);
	}

	@Test
	void formatsWithSymbolAndThousands() {
		assertEquals("$1,050.99", Money.format(105099, "$"));
		assertEquals("$0.00", Money.format(0, "$"));
		assertEquals("$5.00", Money.format(500, "$"));
	}
}
