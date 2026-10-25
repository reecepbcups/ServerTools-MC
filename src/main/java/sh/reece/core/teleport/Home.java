package sh.reece.core.teleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Locale;
import java.util.regex.Pattern;

/** A saved home. Holds the world by name so homes in unloaded worlds still load and save. */
record Home(String name, String world, double x, double y, double z, float yaw, float pitch) {

    static final String DEFAULT_NAME = "home";

    // names end up as yaml keys and in click commands, so keep them plain
    private static final Pattern VALID_NAME = Pattern.compile("[a-z0-9_-]{1,16}");

    static Home of(final String name, final Location location) {
        return new Home(name, location.getWorld().getName(), location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
    }

    static String normalize(final String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    static boolean validName(final String name) {
        return VALID_NAME.matcher(name).matches();
    }

    /** Null when the world isn't loaded. */
    Location location() {
        final World loaded = Bukkit.getWorld(this.world);
        return loaded == null ? null : new Location(loaded, this.x, this.y, this.z, this.yaw, this.pitch);
    }

    String serialize() {
        return String.format(Locale.ROOT, "%s;%.3f;%.3f;%.3f;%.2f;%.2f",
                this.world, this.x, this.y, this.z, this.yaw, this.pitch);
    }

    static Home parse(final String name, final String raw) {
        final String[] parts = raw.split(";");
        if (parts.length != 6) {
            throw new IllegalArgumentException("expected world;x;y;z;yaw;pitch, got " + raw);
        }
        return new Home(name, parts[0],
                Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]),
                Float.parseFloat(parts[4]), Float.parseFloat(parts[5]));
    }
}
