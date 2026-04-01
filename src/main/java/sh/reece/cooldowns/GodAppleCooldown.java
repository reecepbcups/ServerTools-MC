package sh.reece.cooldowns;

import org.bukkit.Material;

import sh.reece.tools.Main;

public class GodAppleCooldown extends ConsumeCooldown {

	public GodAppleCooldown(final Main instance) {
		super(instance, "Cooldowns.GodAppleCooldown", Material.GOLDEN_APPLE, (short) 1);
	}
}
