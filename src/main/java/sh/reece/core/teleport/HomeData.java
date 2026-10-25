package sh.reece.core.teleport;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Every home lives in memory. {@code Homes.yml} is only read on start and only written when something changed,
 * off the main thread, so commands never touch the disk. Concurrent maps because Folia runs commands per region.
 */
final class HomeData {

    private static final Logger LOGGER = LoggerFactory.getLogger(HomeData.class);

    static final String FILE = "Homes.yml";

    private final File file;
    private final Map<UUID, Map<String, Home>> homes = new ConcurrentHashMap<>();
    private volatile boolean dirty;

    HomeData(final File file) {
        this.file = file;
    }

    void load() {
        this.homes.clear();
        final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(this.file);
        int count = 0;
        for (final String id : yaml.getKeys(false)) {
            final ConfigurationSection section = yaml.getConfigurationSection(id);
            final UUID uuid;
            try {
                uuid = UUID.fromString(id);
            } catch (final IllegalArgumentException e) {
                LOGGER.warn("Skipping bad player id {} in {}", id, FILE);
                continue;
            }
            if (section == null) continue;

            for (final String name : section.getKeys(false)) {
                try {
                    put(uuid, Home.parse(name, section.getString(name, "")));
                    count++;
                } catch (final RuntimeException e) {
                    LOGGER.warn("Skipping home {} of {}: {}", name, id, e.getMessage());
                }
            }
        }
        LOGGER.info("Loaded {} homes for {} players", count, this.homes.size());
    }

    /** Read only view, empty if the player has none. */
    Map<String, Home> homes(final UUID id) {
        final Map<String, Home> owned = this.homes.get(id);
        return owned == null ? Map.of() : Collections.unmodifiableMap(owned);
    }

    Optional<Home> home(final UUID id, final String name) {
        return Optional.ofNullable(homes(id).get(name));
    }

    void set(final UUID id, final Home home) {
        put(id, home);
        this.dirty = true;
    }

    boolean delete(final UUID id, final String name) {
        final Map<String, Home> owned = this.homes.get(id);
        if (owned == null || owned.remove(name) == null) {
            return false;
        }
        if (owned.isEmpty()) {
            this.homes.remove(id, owned);
        }
        this.dirty = true;
        return true;
    }

    private void put(final UUID id, final Home home) {
        this.homes.computeIfAbsent(id, k -> new ConcurrentHashMap<>()).put(home.name(), home);
    }

    /**
     * Writes the file if anything changed. Goes through a temp file and a rename so a crash mid write
     * never leaves a half written Homes.yml. Synchronized so two saves never interleave.
     */
    synchronized void saveIfDirty() {
        if (!this.dirty) return;
        this.dirty = false;

        final YamlConfiguration yaml = new YamlConfiguration();
        this.homes.forEach((id, owned) -> owned.forEach((name, home) -> yaml.set(id + "." + name, home.serialize())));

        final File tmp = new File(this.file.getPath() + ".tmp");
        try {
            yaml.save(tmp);
            try {
                Files.move(tmp.toPath(), this.file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (final AtomicMoveNotSupportedException e) {
                Files.move(tmp.toPath(), this.file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (final IOException e) {
            this.dirty = true;
            LOGGER.error("Failed to save {}", FILE, e);
        }
    }
}
