package sh.reece.core.warp.v2;

import com.google.common.base.Preconditions;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.reece.tools.ConfigUtils;
import sh.reece.utiltools.Util;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public record Warp(String name, String permission, Location location) {

    private static final Logger LOGGER = LoggerFactory.getLogger(Warp.class);

    public static final String WARPS_FILE = "Warps.yml";

    public Warp {
        location = location.clone();
        permission = permission == null ? "" : permission;
        Preconditions.checkArgument(location.getWorld() != null, "the provided location must not be null");
    }

    @Override
    public Location location() {
        return location.clone();
    }

    public CompletableFuture<Boolean> teleport(Player player) {
        if (permission.isBlank() || !player.hasPermission(permission)) {
            return CompletableFuture.completedFuture(false);
        }

        return player.teleportAsync(this.location);
    }

    public static void saveAll(FileConfiguration config, List<Warp> warps, boolean forceSave) {
        for (final Warp warp : warps) {
            save(config, warp, false);
        }

        if (forceSave) {
            ConfigUtils.getInstance().saveConfig(config, WARPS_FILE);
        }
    }

    public static void save(FileConfiguration config, Warp warp, boolean forceSave) {
        ConfigurationSection warpSection = config.createSection(warp.name());
        warpSection.set("permission", warp.permission());
        warpSection.set("location", Util.locationToString(warp.location()));
        if (forceSave) {
            ConfigUtils.getInstance().saveConfig(config, WARPS_FILE);
        }
    }

    public static void delete(FileConfiguration config, Warp warp, boolean forceSave) {
        config.set(warp.name(), null);
        if (forceSave) {
            ConfigUtils.getInstance().saveConfig(config, WARPS_FILE);
        }
    }

    public static List<Warp> parseWarps(ConfigurationSection root) {
        return root.getKeys(false).stream()
                .map((key) -> parseWarp(key, root.getConfigurationSection(key)))
                .flatMap(Optional::stream)
                .toList();
    }

    public static Optional<Warp> parseWarp(String sectionName, ConfigurationSection section) {
        if (section == null) {
            LOGGER.warn("Skipping warp {}: not a configuration section", sectionName);
            return Optional.empty();
        }

        try {
            return Optional.of(new Warp(
                    sectionName,
                    section.getString("permission", ""),
                    Util.stringToLocation(section.getString("location"))
            ));
        } catch (final RuntimeException e) {
            LOGGER.warn("Skipping warp {}", sectionName, e);
            return Optional.empty();
        }
    }
}
