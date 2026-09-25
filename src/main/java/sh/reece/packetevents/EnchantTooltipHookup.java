package sh.reece.packetevents;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;

/**
 * Isolates every PacketEvents-typed reference for the enchant tooltip rewriter.
 * Lives outside Main and outside any package Loader auto-scans - referencing a
 * PacketEvents type in a field/method signature makes the JVM require PacketEvents
 * to be present just to load or reflectively inspect the class, even behind a
 * runtime null check.
 */
public final class EnchantTooltipHookup {

	private static PacketListenerCommon listener;

	private EnchantTooltipHookup() {
	}

	public static void register() {
		listener = PacketEvents.getAPI().getEventManager().registerListener(new EnchantTooltipListener());
	}

	public static void unregister() {
		if (listener != null) {
			PacketEvents.getAPI().getEventManager().unregisterListener(listener);
			listener = null;
		}
	}
}
