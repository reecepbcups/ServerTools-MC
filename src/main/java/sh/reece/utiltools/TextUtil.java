package sh.reece.utiltools;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TextUtil {

    private static final char SECTION = '§';
    private static final MiniMessage COLOR_MESSAGE = MiniMessage.builder()
            .tags(TagResolver.builder()
                    .resolver(StandardTags.color())
                    .resolver(StandardTags.gradient())
                    .resolvers(StandardTags.decorations())
                    .build()
            ).build();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character(SECTION)
            .hexColors()
            .build();
    private static final Logger log = LoggerFactory.getLogger(TextUtil.class);

    private TextUtil() {
        throw new UnsupportedOperationException("can not instantiate utility class " + getClass());
    }

    public static void consoleMessage(Component message) {
        Bukkit.getConsoleSender().sendMessage(message);
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
     * @return int confidence, 0 if equal confidence, negative range for legacy, positive range for mini-message.
     *         Note a tie is read as legacy by {@link #color(String, boolean)}.
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
