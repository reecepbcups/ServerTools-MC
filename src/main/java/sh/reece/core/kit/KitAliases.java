package sh.reece.core.kit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Short names for items, enchants and potion effects (dpickaxe, durability, regen).
 * Loaded once from the bundled table on first use, ~50k entries.
 */
final class KitAliases {

    static final String RESOURCE = "/kit-aliases.txt.gz";

    private static final String ENCHANT = "enchant:";
    private static final String EFFECT = "effect:";

    // item alias -> MATERIAL or MATERIAL:SUBTYPE (potion type / spawner entity)
    private final Map<String, String> items = new HashMap<>(65536);
    private final Map<String, String> enchants = new HashMap<>();
    private final Map<String, String> effects = new HashMap<>();

    private static final class Holder {
        static final KitAliases INSTANCE = load();
    }

    static KitAliases get() {
        return Holder.INSTANCE;
    }

    private static KitAliases load() {
        try (InputStream in = KitAliases.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("missing " + RESOURCE);
            }
            return parse(new GZIPInputStream(in));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static KitAliases parse(final InputStream in) throws IOException {
        final KitAliases aliases = new KitAliases();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty() || line.charAt(0) == '#') continue;

            final String[] parts = line.split(" ");
            final String target = parts[0];
            final Map<String, String> into;
            final String value;
            if (target.startsWith(ENCHANT)) {
                into = aliases.enchants;
                value = target.substring(ENCHANT.length());
            } else if (target.startsWith(EFFECT)) {
                into = aliases.effects;
                value = target.substring(EFFECT.length());
            } else {
                into = aliases.items;
                value = target;
                // subtype entries (POTION:LONG_SWIFTNESS) are only reachable by alias, so key the canonical name too
                into.putIfAbsent(target.toLowerCase(Locale.ROOT), value);
            }
            for (int i = 1; i < parts.length; i++) {
                into.putIfAbsent(parts[i], value);
            }
        }
        return aliases;
    }

    /** Item alias to MATERIAL or MATERIAL:SUBTYPE, null when unknown. */
    String item(final String name) {
        return this.items.get(name.toLowerCase(Locale.ROOT));
    }

    /** Enchant alias to its minecraft key, null when unknown. */
    String enchant(final String name) {
        return this.enchants.get(name.toLowerCase(Locale.ROOT));
    }

    /** Potion effect alias to its minecraft key, null when unknown. */
    String effect(final String name) {
        return this.effects.get(name.toLowerCase(Locale.ROOT));
    }
}
