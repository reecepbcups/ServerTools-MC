package sh.reece.core.kit;

import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.List;

/**
 * One parsed kit. Lines without placeholders are turned into items once at load, the rest per claim.
 *
 * @param delaySeconds 0 for none, negative for a one time kit
 * @param lines        the raw lines, kept for /showkit
 * @param source       the file the kit was read from, so /delkit edits the right one
 */
record Kit(String name, double delaySeconds, double cost, List<Entry> entries, List<String> lines, File source) {

    enum Kind { ITEM, MONEY, COMMAND }

    /**
     * @param slot     inventory slot from a slot:N prefix, -1 for anywhere
     * @param template prebuilt item, null when the line has placeholders and must be built per claim
     */
    record Entry(Kind kind, String raw, int slot, ItemStack template) {
    }

    /** 0 when ready, -1 when a one time kit was already used, otherwise the epoch millis it unlocks at. */
    long nextUse(final long lastUsed, final long now) {
        // no record, or a timestamp in the future from a bad clock: give the benefit of the doubt
        if (lastUsed <= 0 || lastUsed > now) return 0;
        if (this.delaySeconds < 0) return -1;

        final long unlock = lastUsed + (long) (this.delaySeconds * 1000);
        return unlock <= now ? 0 : unlock;
    }

    static String sanitizeName(final String name) {
        return name.toLowerCase(java.util.Locale.ROOT).replace('.', '_').replace('/', '_');
    }
}
