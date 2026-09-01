package sh.reece.core.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import sh.reece.core.economy.EconomyStorage.Result;

/** Exercises the money movement logic that /pay, /eco and Vault all sit on top of. */
class EconomyStorageTest {

	private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-000000000a11");
	private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-000000000b0b");
	private static final UUID CAROL = UUID.fromString("00000000-0000-0000-0000-00000000ca01");

	private EconomyStorage fresh(long startingCents) throws Exception {
		EconomyStorage s = new EconomyStorage("jdbc:sqlite::memory:", startingCents);
		s.open();
		return s;
	}

	// --- accounts -----------------------------------------------------------

	@Test
	void newAccountGetsStartingBalance() throws Exception {
		EconomyStorage s = fresh(50000); // $500 start
		assertFalse(s.has(ALICE));
		assertTrue(s.createAccount(ALICE, "Alice"));
		assertTrue(s.has(ALICE));
		assertEquals(50000, s.getCents(ALICE));
		assertFalse(s.createAccount(ALICE, "Alice"), "second create is a no-op");
	}

	@Test
	void unknownAccountReadsZero() throws Exception {
		EconomyStorage s = fresh(0);
		assertEquals(0, s.getCents(ALICE));
		assertFalse(s.has(ALICE));
	}

	// --- pay / transfer: the headline cases ---------------------------------

	@Test
	void payMovesMoneyBothWays() throws Exception {
		EconomyStorage s = fresh(0);
		s.deposit(ALICE, "Alice", 10000); // $100
		assertEquals(Result.SUCCESS, s.transfer(ALICE, "Alice", BOB, "Bob", 2500)); // pay $25
		assertEquals(7500, s.getCents(ALICE));
		assertEquals(2500, s.getCents(BOB));
	}

	@Test
	void payWithCents() throws Exception {
		EconomyStorage s = fresh(0);
		s.deposit(ALICE, "Alice", 1000); // $10.00
		assertEquals(Result.SUCCESS, s.transfer(ALICE, "Alice", BOB, "Bob", 99)); // pay $0.99
		assertEquals(901, s.getCents(ALICE));
		assertEquals(99, s.getCents(BOB));
	}

	@Test
	void payCreatesPayeeAtStartingBalance() throws Exception {
		EconomyStorage s = fresh(20000); // everyone starts at $200
		s.set(ALICE, "Alice", 30000);
		// Bob never had an account; transfer should treat him as starting balance then add
		assertEquals(Result.SUCCESS, s.transfer(ALICE, "Alice", BOB, "Bob", 5000));
		assertEquals(25000, s.getCents(ALICE));
		assertEquals(25000, s.getCents(BOB)); // 200 start + 50
	}

	@Test
	void payFailsWhenBroke() throws Exception {
		EconomyStorage s = fresh(0);
		s.deposit(ALICE, "Alice", 500); // $5
		assertEquals(Result.INSUFFICIENT_FUNDS, s.transfer(ALICE, "Alice", BOB, "Bob", 600));
		assertEquals(500, s.getCents(ALICE), "sender untouched on failure");
		assertEquals(0, s.getCents(BOB), "payee untouched on failure");
	}

	@Test
	void payExactBalanceSucceeds() throws Exception {
		EconomyStorage s = fresh(0);
		s.deposit(ALICE, "Alice", 500);
		assertEquals(Result.SUCCESS, s.transfer(ALICE, "Alice", BOB, "Bob", 500));
		assertEquals(0, s.getCents(ALICE));
		assertEquals(500, s.getCents(BOB));
	}

	@Test
	void payZeroOrNegativeRejected() throws Exception {
		EconomyStorage s = fresh(0);
		s.deposit(ALICE, "Alice", 1000);
		assertEquals(Result.INVALID_AMOUNT, s.transfer(ALICE, "Alice", BOB, "Bob", 0));
		assertEquals(Result.INVALID_AMOUNT, s.transfer(ALICE, "Alice", BOB, "Bob", -100));
		assertEquals(1000, s.getCents(ALICE));
	}

	@Test
	void payYourselfRejected() throws Exception {
		EconomyStorage s = fresh(0);
		s.deposit(ALICE, "Alice", 1000);
		assertEquals(Result.SAME_ACCOUNT, s.transfer(ALICE, "Alice", ALICE, "Alice", 100));
		assertEquals(1000, s.getCents(ALICE), "no money created or destroyed");
	}

	@Test
	void payOverflowRejectedAndNobodyCharged() throws Exception {
		EconomyStorage s = fresh(0);
		s.set(ALICE, "Alice", 5000);
		s.set(BOB, "Bob", Money.MAX_CENTS); // Bob already at the ceiling
		assertEquals(Result.OVERFLOW, s.transfer(ALICE, "Alice", BOB, "Bob", 5000));
		assertEquals(5000, s.getCents(ALICE), "sender not charged when payee would overflow");
		assertEquals(Money.MAX_CENTS, s.getCents(BOB));
	}

	// --- admin ops ----------------------------------------------------------

	@Test
	void depositWithdrawSet() throws Exception {
		EconomyStorage s = fresh(0);
		assertEquals(Result.SUCCESS, s.deposit(ALICE, "Alice", 1000));
		assertEquals(1000, s.getCents(ALICE));
		assertEquals(Result.SUCCESS, s.withdraw(ALICE, "Alice", 400));
		assertEquals(600, s.getCents(ALICE));
		assertEquals(Result.SUCCESS, s.set(ALICE, "Alice", 12345));
		assertEquals(12345, s.getCents(ALICE));
	}

	@Test
	void withdrawMoreThanBalanceRejected() throws Exception {
		EconomyStorage s = fresh(0);
		s.deposit(ALICE, "Alice", 100);
		assertEquals(Result.INSUFFICIENT_FUNDS, s.withdraw(ALICE, "Alice", 101));
		assertEquals(100, s.getCents(ALICE));
	}

	@Test
	void depositOverflowRejected() throws Exception {
		EconomyStorage s = fresh(0);
		s.set(ALICE, "Alice", Money.MAX_CENTS);
		assertEquals(Result.OVERFLOW, s.deposit(ALICE, "Alice", 1));
		assertEquals(Money.MAX_CENTS, s.getCents(ALICE));
	}

	@Test
	void setNegativeRejected() throws Exception {
		EconomyStorage s = fresh(0);
		assertEquals(Result.INVALID_AMOUNT, s.set(ALICE, "Alice", -1));
	}

	// --- no drift over many operations --------------------------------------

	@Test
	void thousandTinyPaysDoNotDrift() throws Exception {
		EconomyStorage s = fresh(0);
		s.deposit(ALICE, "Alice", 1000); // $10.00 = 1000 one-cent pays
		for (int i = 0; i < 1000; i++) {
			assertEquals(Result.SUCCESS, s.transfer(ALICE, "Alice", BOB, "Bob", 1));
		}
		assertEquals(0, s.getCents(ALICE));
		assertEquals(1000, s.getCents(BOB));
	}

	@Test
	void totalMoneyConservedAcrossRandomishPays() throws Exception {
		EconomyStorage s = fresh(0);
		s.set(ALICE, "Alice", 100000);
		s.set(BOB, "Bob", 50000);
		s.set(CAROL, "Carol", 25000);
		long total = 175000;
		s.transfer(ALICE, "Alice", BOB, "Bob", 33333);
		s.transfer(BOB, "Bob", CAROL, "Carol", 12121);
		s.transfer(CAROL, "Carol", ALICE, "Alice", 999);
		assertEquals(total, s.getCents(ALICE) + s.getCents(BOB) + s.getCents(CAROL));
	}

	// --- persistence + ordering ---------------------------------------------

	@Test
	void balancesSurviveReopen(@TempDir Path dir) throws Exception {
		File db = new File(dir.toFile(), "eco.db");
		String url = "jdbc:sqlite:" + db.getAbsolutePath();

		EconomyStorage s1 = new EconomyStorage(url, 0);
		s1.open();
		s1.deposit(ALICE, "Alice", 4200);
		s1.close();

		EconomyStorage s2 = new EconomyStorage(url, 0);
		s2.open();
		assertEquals(4200, s2.getCents(ALICE), "balance loaded from disk");
		s2.close();
	}

	@Test
	void baltopOrdersHighestFirst() throws Exception {
		EconomyStorage s = fresh(0);
		s.set(ALICE, "Alice", 300);
		s.set(BOB, "Bob", 900);
		s.set(CAROL, "Carol", 600);
		List<UUID> order = List.copyOf(s.top(10).keySet());
		assertEquals(List.of(BOB, CAROL, ALICE), order);
	}
}
