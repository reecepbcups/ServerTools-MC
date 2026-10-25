package sh.reece.core.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Cooldown math and the bundled alias table, the parts that don't need a running server. */
class KitTest {

	private static Kit kit(double delaySeconds) {
		return new Kit("test", delaySeconds, 0, List.of(), List.of(), null);
	}

	@Test
	void neverUsedIsReady() {
		assertEquals(0, kit(600).nextUse(0, 1_000_000));
		assertEquals(0, kit(-1).nextUse(0, 1_000_000));
	}

	@Test
	void futureTimestampIsReady() {
		assertEquals(0, kit(600).nextUse(2_000_000, 1_000_000));
	}

	@Test
	void oneTimeKitLocksForever() {
		assertEquals(-1, kit(-1).nextUse(500, 1_000_000_000));
	}

	@Test
	void timedKitUnlocksAfterDelay() {
		assertEquals(1_600_000, kit(600).nextUse(1_000_000, 1_000_001));
		assertEquals(0, kit(600).nextUse(1_000_000, 1_600_000));
		assertEquals(0, kit(0).nextUse(1_000_000, 1_000_000));
	}

	@Test
	void fractionalDelay() {
		assertEquals(1_001_500, kit(1.5).nextUse(1_000_000, 1_000_100));
	}

	@Test
	void sanitizesNames() {
		assertEquals("my_kit_x", Kit.sanitizeName("My.Kit/X"));
	}

	@Test
	void underscoresBecomeSpaces() {
		assertEquals("Top Hat", KitItems.text("Top_Hat"));
		assertEquals("snake_case", KitItems.text("snake\\_case"));
	}

	@Test
	void resolvesBundledAliases() {
		KitAliases aliases = KitAliases.get();
		assertEquals("DIAMOND_PICKAXE", aliases.item("dpickaxe"));
		assertEquals("STONE_SWORD", aliases.item("StoneSword"));
		assertEquals("LEATHER_HELMET", aliases.item("lhelm"));
		assertEquals("POTION:LONG_SWIFTNESS", aliases.item("potionofspeedlong"));
		assertEquals("SPAWNER:ALLAY", aliases.item("allayspawner"));
		assertEquals("unbreaking", aliases.enchant("durability"));
		assertEquals("efficiency", aliases.enchant("digspeed"));
		assertEquals("regeneration", aliases.effect("regen"));
		assertNull(aliases.item("not_a_real_item"));
	}
}
