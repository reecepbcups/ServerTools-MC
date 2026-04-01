package sh.reece.cooldowns;

import org.bukkit.Material;

import sh.reece.tools.Main;

public class GoldenAppleCooldown extends ConsumeCooldown {

	public GoldenAppleCooldown(final Main instance) {
		super(instance, "Cooldowns.GoldenAppleCooldown", Material.GOLDEN_APPLE, (short) 0);
	}
}
