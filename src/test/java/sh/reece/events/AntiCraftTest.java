package sh.reece.events;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

/**
 * Covers the station-blocking decision shared by the anvil / smithing /
 * grindstone / enchant handlers - given a station's ban list and an output
 * item, should the result be removed?
 */
class AntiCraftTest {

	@Test
	void blocksWhenOutputTypeIsInBanList() {
		Set<Material> banned = EnumSet.of(Material.DIAMOND_CHESTPLATE);
		assertTrue(AntiCraft.isStationBlocked(banned, new ItemStack(Material.DIAMOND_CHESTPLATE)));
	}

	@Test
	void allowsWhenOutputTypeIsNotInBanList() {
		Set<Material> banned = EnumSet.of(Material.DIAMOND_CHESTPLATE);
		assertFalse(AntiCraft.isStationBlocked(banned, new ItemStack(Material.STONE)));
	}

	@Test
	void allowsWhenBanListIsEmpty() {
		Set<Material> banned = EnumSet.noneOf(Material.class);
		assertFalse(AntiCraft.isStationBlocked(banned, new ItemStack(Material.DIAMOND_CHESTPLATE)));
	}

	@Test
	void allowsWhenThereIsNoOutput() {
		Set<Material> banned = EnumSet.of(Material.DIAMOND_CHESTPLATE);
		assertFalse(AntiCraft.isStationBlocked(banned, null));
	}
}
