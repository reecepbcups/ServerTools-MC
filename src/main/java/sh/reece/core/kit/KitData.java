package sh.reece.core.kit;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Last claim time per player per kit. Held in memory and written to {@code KitData.yml} only when something changed.
 * Maps are concurrent because on Folia two players claim kits from different region threads.
 */
final class KitData {

    private static final Logger LOGGER = LoggerFactory.getLogger(KitData.class);

    static final String FILE = "KitData.yml";

    private final File file;
    private final Map<UUID, Map<String, Long>> used = new ConcurrentHashMap<>();
    private volatile boolean dirty;

    KitData(final File file) {
        this.file = file;
    }

    boolean exists() {
        return this.file.exists();
    }

    void load() {
        final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(this.file);
        for (final String id : yaml.getKeys(false)) {
            final ConfigurationSection kits = yaml.getConfigurationSection(id);
            if (kits == null) continue;
            try {
                readKits(UUID.fromString(id), kits);
            } catch (final IllegalArgumentException e) {
                LOGGER.warn("Skipping bad player id {} in {}", id, FILE);
            }
        }
    }

    /** Pulls {@code timestamps.kits} out of every userdata file, keeping anything already recorded here. */
    int importUserdata(final File userdataDir) {
        final File[] files = userdataDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return 0;

        int imported = 0;
        for (final File userFile : files) {
            final UUID id;
            try {
                id = UUID.fromString(userFile.getName().substring(0, userFile.getName().length() - 4));
            } catch (final IllegalArgumentException e) {
                continue;
            }
            final ConfigurationSection kits = YamlConfiguration.loadConfiguration(userFile)
                    .getConfigurationSection("timestamps.kits");
            if (kits != null && readKits(id, kits)) {
                imported++;
            }
        }
        if (imported > 0) {
            this.dirty = true;
        }
        return imported;
    }

    private boolean readKits(final UUID id, final ConfigurationSection kits) {
        boolean any = false;
        for (final String kit : kits.getKeys(false)) {
            final long time = kits.getLong(kit);
            if (time > 0) {
                this.used.computeIfAbsent(id, k -> new ConcurrentHashMap<>()).putIfAbsent(Kit.sanitizeName(kit), time);
                any = true;
            }
        }
        return any;
    }

    long lastUsed(final UUID id, final String kit) {
        final Map<String, Long> kits = this.used.get(id);
        return kits == null ? 0 : kits.getOrDefault(kit, 0L);
    }

    /** 0 clears the record, which is what /kitreset does. */
    void setLastUsed(final UUID id, final String kit, final long time) {
        if (time <= 0) {
            final Map<String, Long> kits = this.used.get(id);
            if (kits != null) kits.remove(kit);
        } else {
            this.used.computeIfAbsent(id, k -> new ConcurrentHashMap<>()).put(kit, time);
        }
        this.dirty = true;
    }

    /** Writes the file if anything changed. Safe from any thread, synchronized so two saves never interleave. */
    synchronized void saveIfDirty() {
        if (!this.dirty) return;
        this.dirty = false;

        final YamlConfiguration yaml = new YamlConfiguration();
        this.used.forEach((id, kits) -> kits.forEach((kit, time) -> yaml.set(id + "." + kit, time)));
        try {
            yaml.save(this.file);
        } catch (final IOException e) {
            this.dirty = true;
            LOGGER.error("Failed to save {}", FILE, e);
        }
    }
}
