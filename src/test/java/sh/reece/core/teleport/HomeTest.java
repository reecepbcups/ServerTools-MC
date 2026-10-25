package sh.reece.core.teleport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Limits, parsing and the save/load round trip, the parts that don't need a running server. */
class HomeTest {

	private static Map<String, Integer> tiers(Object... pairs) {
		Map<String, Integer> map = new LinkedHashMap<>();
		for (int i = 0; i < pairs.length; i += 2) {
			map.put((String) pairs[i], (Integer) pairs[i + 1]);
		}
		return map;
	}

	@Test
	void limitPicksHighestHeldTier() {
		Map<String, Integer> tiers = tiers("default", 3, "vip", 5, "mvp", 10);
		assertEquals(3, HomeCommand.limit(tiers, tier -> false));
		assertEquals(5, HomeCommand.limit(tiers, Set.of("vip")::contains));
		assertEquals(10, HomeCommand.limit(tiers, Set.of("vip", "mvp")::contains));
	}

	@Test
	void negativeTierIsUnlimited() {
		Map<String, Integer> tiers = tiers("default", 3, "staff", -1);
		assertEquals(-1, HomeCommand.limit(tiers, Set.of("staff")::contains));
		assertEquals(-1, HomeCommand.limit(tiers("default", -1), tier -> false));
	}

	@Test
	void missingDefaultIsOne() {
		assertEquals(1, HomeCommand.limit(tiers(), tier -> false));
	}

	@Test
	void validNames() {
		assertTrue(Home.validName("home"));
		assertTrue(Home.validName("my_base-2"));
		assertFalse(Home.validName(""));
		assertFalse(Home.validName("a.b"));
		assertFalse(Home.validName("x".repeat(17)));
	}

	@Test
	void serializeRoundTrip() {
		Home home = new Home("base", "world", 1.5, 64, -20.25, 90f, -10f);
		assertEquals(home, Home.parse("base", home.serialize()));
	}

	@Test
	void parseRejectsGarbage() {
		assertThrows(IllegalArgumentException.class, () -> Home.parse("x", "world;1;2"));
	}

	@Test
	void dataSavesOnlyWhenDirtyAndReloads(@TempDir Path dir) {
		File file = dir.resolve(HomeData.FILE).toFile();
		UUID id = UUID.randomUUID();

		HomeData data = new HomeData(file);
		data.saveIfDirty();
		assertFalse(file.exists());

		data.set(id, new Home("base", "world", 1, 2, 3, 0, 0));
		data.set(id, new Home("farm", "world_nether", 4, 5, 6, 0, 0));
		assertTrue(data.delete(id, "farm"));
		data.saveIfDirty();
		assertTrue(file.exists());

		HomeData reloaded = new HomeData(file);
		reloaded.load();
		assertEquals(Set.of("base"), reloaded.homes(id).keySet());
		assertEquals(3, reloaded.home(id, "base").orElseThrow().z());
	}
}
