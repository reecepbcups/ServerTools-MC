package sh.reece.utiltools;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public final class TextUtil {

    private static final char SECTION = '§';
    private static final MiniMessage COLOR_MESSAGE = MiniMessage.builder().tags(TagResolver.builder().resolver(StandardTags.color()).resolver(StandardTags.gradient()).resolvers(StandardTags.decorations()).build()).build();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder().character(SECTION).hexColors().build();
    private static final Logger log = LoggerFactory.getLogger(TextUtil.class);

    private TextUtil() {
        throw new UnsupportedOperationException("can not instantiate utility class " + getClass());
    }

    public static void consoleMessage(Component message) {
        Bukkit.getConsoleSender().sendMessage(message);
    }

    /**
     * Parses a string and sends it to the console.
     * <p>
     * The non-legacy counterpart to {@code Util.consoleMSG(String)}, which parses its argument as legacy only. Parsing
     * runs through {@link #color(String)}, so a mini-message string is read as one while a legacy string still renders
     * - call sites can move over one at a time.
     * <p>
     * Note that {@link #color(String)} reads a tie as legacy, so a mini-message string needs at least one more
     * {@literal <} than it has ampersands to be recognised. In practice any string carrying a real tag clears that
     * bar.
     *
     * @param message the mini-message (or legacy) string to parse and send
     */
    public static void consoleMessage(String message) {
        consoleMessage(color(message));
    }

    /**
     * Parses a string and sends it to the console, optionally logging a nag when the string turns out to be legacy.
     *
     * @param message   the mini-message (or legacy) string to parse and send
     * @param nagLegacy whether to log a warning if the string is read as legacy
     * @see #color(String, boolean)
     */
    public static void consoleMessage(String message, boolean nagLegacy) {
        consoleMessage(color(message, nagLegacy));
    }

    /**
     * Parses a mini-message (or legacy) template whose {@code %s} placeholders are filled from {@code placeholders}, in
     * {@link String#format(java.util.Locale, String, Object...)} order.
     * <p>
     * Substitution happens before parsing, so each value is escaped first: a placeholder carrying something like
     * {@literal <red>} renders as those characters rather than opening a tag. Only the template may contain markup,
     * which is what makes this safe for user supplied values - warp names, player names, config keys.
     * <p>
     * Formatting uses {@link java.util.Locale#ROOT} so numeric values render the same everywhere, rather than picking
     * up a comma decimal separator from the server's default locale.
     *
     * @param template     the mini-message (or legacy) template
     * @param placeholders the values to substitute, escaped before parsing
     * @return the parsed component
     */
    public static Component format(String template, Object... placeholders) {
        if (placeholders.length == 0) {
            return color(template);
        }

        Object[] escaped = new Object[placeholders.length];
        for (int i = 0; i < placeholders.length; i++) {
            escaped[i] = COLOR_MESSAGE.escapeTags(String.valueOf(placeholders[i]));
        }

        return color(String.format(Locale.ROOT, template, escaped));
    }

    /**
     * Converts a legacy string into a component
     *
     * @param legacy the legacy string
     * @return the adventure component
     */
    public static Component legacyToComponent(String legacy) {
        legacy = Util.color(legacy);
        return LEGACY_SERIALIZER.deserialize(legacy);
    }

    /**
     * Converts a mini-message or legacy string into a component
     * <p>
     * Determiniation between mini-message and legacy are done on a best guess basis with legacy winning out on a tie,
     * e.g. a string like {@literal <blue>My &3String is cool} will favor legacy leaving the tag dangling.
     * <p>
     * Legacy is the conservative guess: a genuine mini-message string leans on {@literal <} for every tag, so it
     * usually outnumbers any stray ampersand, while a legacy string that happens to contain one {@literal <} would
     * otherwise be parsed as mini-message and lose its colour codes entirely.
     * <p>
     * By default this method will not nag via logs when passed a legacy string see {@link #color(String, boolean)} to
     * nag the user. Best to nag on start up.
     *
     * @param string the string to parse
     * @return the parsed component
     */
    public static Component color(String string) {
        return color(string, false);
    }

    /**
     * Converts a mini-message or legacy string into a component
     * <p>
     * Determiniation between mini-message and legacy are done on a best guess basis with legacy winning out on a tie,
     * e.g. a string like {@literal <blue>My &3String is cool} will favor legacy leaving the tag dangling.
     * <p>
     * Legacy is the conservative guess: a genuine mini-message string leans on {@literal <} for every tag, so it
     * usually outnumbers any stray ampersand, while a legacy string that happens to contain one {@literal <} would
     * otherwise be parsed as mini-message and lose its colour codes entirely.
     *
     * @param string    the string to parse
     * @param nagLegacy whether or not to log a nag if legacy is detected
     * @return the parsed component
     */
    public static Component color(String string, boolean nagLegacy) {
        int leaning = isLegacyOrMiniMessage(string);
        if (leaning <= 0) {
            // only nag when ampersands actually turned up - a tie is often just plain text
            if (nagLegacy && leaning < 0) {
                log.warn("Parsed legacy string {}, warning use of legacy strings is not encouraged please use mini message see https://docs.papermc.io/adventure/minimessage/", string);
            }
            return legacyToComponent(string);
        }

        return COLOR_MESSAGE.deserialize(string);
    }

    /**
     * Tries best to determine if legacy or mini-message string
     * <p>
     * Occurance increments are made by 1 for each, where 0 indicates a tie.
     *
     * @param string the string to assess
     * @return int confidence, 0 if equal confidence, negative range for legacy, positive range for mini-message. Note a
     * tie is read as legacy by {@link #color(String, boolean)}.
     */
    private static int isLegacyOrMiniMessage(String string) {
        int confidence = 0;
        for (int i = 0; i < string.length(); i++) {
            int c = string.charAt(i);
            switch (c) {
                case '&' -> confidence--;
                case '<' -> confidence++;
            }
        }

        return confidence;
    }
}
