package sh.reece.core.teleport;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Warmup, cooldown and cost for one kind of teleport, read from its config section.
 * The section path doubles as the cooldown key, so /home and /rtp cool down separately.
 */
public record TeleportAction(String key, int warmupSeconds, int cooldownSeconds, double cost) {

    /** For staff sending someone else, no wait and nothing charged. */
    public static final TeleportAction INSTANT = new TeleportAction("instant", 0, 0, 0);

    public static TeleportAction fromConfig(final FileConfiguration config, final String section) {
        return new TeleportAction(section,
                Math.max(0, config.getInt(section + ".Warmup", 0)),
                Math.max(0, config.getInt(section + ".Cooldown", 0)),
                Math.max(0, config.getDouble(section + ".Cost", 0)));
    }
}
