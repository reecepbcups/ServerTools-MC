package sh.reece.core.hologram;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.utiltools.TextUtil;

import java.util.ArrayList;
import java.util.List;

import static net.kyori.adventure.text.Component.text;

/**
 * One hologram from {@code Holograms.yml}, and the displays that draw it.
 */
public record Hologram(String key, List<Component> lines, Location location) {

    public static final String LOCATION_KEY = "location";
    public static final String TEXT_KEY = "lines";
    public static final NamespacedKey IDENTIFIER_KEY = new NamespacedKey("servertools", "hologram");

    /**
     * Stored Y values were authored against an ArmorStand nameplate, which floated ~2 above it.
     */
    public static final double Y_OFFSET = 2.0D;
    private static final double LINE_SPACING = 0.25D;
    private static final double SWEEP_XZ = 2.0D;
    private static final double SWEEP_Y = 8.0D;

    public Hologram {
        lines = List.copyOf(lines);
        location = location.clone().subtract(0, Y_OFFSET, 0);
    }

    @Override
    public Location location() {
        return location.clone();
    }

    /**
     * Redraws this hologram on the region that owns it, replacing any displays it already has.
     *
     * @return false if the world is not loaded; true only means the draw was scheduled
     */
    public boolean spawn(final Plugin plugin) {
        return spawn(plugin, false);
    }

    /**
     * @param sweepLegacy also removes untagged displays drawn by the pre-rewrite class; done once on the
     *                    first load after the version bump, see {@link HologramFeature#load()}
     */
    public boolean spawn(final Plugin plugin, final boolean sweepLegacy) {
        final World world = this.location.getWorld();
        if (world == null) {
            return false;
        }

        loadLocationAndExecute(plugin, () -> {
            erase(world);
            if (sweepLegacy) {
                eraseLegacy(world);
            }
            draw(world);
        });
        return true;
    }

    /**
     * Removes only displays tagged with {@link #IDENTIFIER_KEY} for this key.
     */
    public void despawn(final Plugin plugin) {
        final World world = this.location.getWorld();
        if (world == null) {
            return;
        }

        loadLocationAndExecute(plugin, () -> erase(world));
    }

    private void draw(final World world) {
        Location current = this.location();
        for (final Component line : this.lines) {
            current = current.clone().subtract(0, LINE_SPACING, 0);

            // a blank line is a gap: it moves the cursor down but gets no entity
            if (Component.empty().equals(line)) {
                continue;
            }

            world.spawn(current, TextDisplay.class, (display) -> {
                display.setBillboard(Display.Billboard.CENTER);
                display.text(line);
                display.getPersistentDataContainer().set(IDENTIFIER_KEY, PersistentDataType.STRING, this.key);
            });
        }
    }

    private void erase(final World world) {
        world.getNearbyEntitiesByType(TextDisplay.class, this.location, SWEEP_XZ, SWEEP_Y, (display) -> {
            final PersistentDataContainer container = display.getPersistentDataContainer();
            return this.key.equals(container.get(IDENTIFIER_KEY, PersistentDataType.STRING));
        }).forEach(Entity::remove);
    }

    private void eraseLegacy(final World world) {
        world.getNearbyEntities(this.location, SWEEP_XZ, SWEEP_Y, SWEEP_XZ, (entity) -> {
            PersistentDataContainer container = entity.getPersistentDataContainer();
            if (container.has(IDENTIFIER_KEY)) {
                return false;
            }

            // same predicate the old class used to clean up after itself: its TextDisplays
            // carried no tag, and the ArmorStand-era holos had a visible name
            return entity instanceof TextDisplay
                    || (entity instanceof ArmorStand && entity.isCustomNameVisible());
        }).forEach(Entity::remove);
    }

    private void loadLocationAndExecute(final Plugin plugin, final Runnable runnable) {
        World world = this.location.getWorld();
        if (world == null) {
            return;
        }

        world.getChunkAtAsync(this.location).whenComplete((chunk, exception) -> {
            if (exception != null) {
                throw new RuntimeException(exception);
            }

            Bukkit.getRegionScheduler().execute(plugin, this.location, runnable);
        });
    }

    /**
     * Parses each key on its own, so a malformed entry costs only itself.
     */
    public static List<Hologram> parseConfig(final FileConfiguration config) {
        final List<Hologram> holograms = new ArrayList<>();
        for (final String key : config.getKeys(false)) {
            try {
                holograms.add(parse(key, config.getConfigurationSection(key)));
            } catch (final RuntimeException e) {
                warn(key, "could not be read, skipping it: " + e);
            }
        }

        return holograms;
    }

    public static Hologram parse(final String key, final ConfigurationSection section) {
        final String[] locationParts = section.getString(LOCATION_KEY).trim().split(",");
        final World world = Bukkit.getWorld(locationParts[0]);
        final Location location = new Location(world, Double.parseDouble(locationParts[1]), Double.parseDouble(locationParts[2]), Double.parseDouble(locationParts[3]));

        final List<Component> lines = section.getStringList(TEXT_KEY).stream().map(Hologram::render).toList();

        return new Hologram(key, lines, location);
    }

    static void warn(final String key, final String problem) {
        TextUtil.consoleMessage(TextUtil.color("<red>[!] Hologram <white>").append(text(key)).append(TextUtil.color("<red> ")).append(text(problem)));
    }

    private static Component render(final String line) {
        String text = ConfigUtils.replaceVariable(line);
        if (Main.isPAPIEnabled()) {
            text = PlaceholderAPI.setPlaceholders(null, text);
        }

        // don't nag: hologram lines are user config authored in legacy '&' codes by design
        return text.isEmpty() ? Component.empty() : TextUtil.color(text, false);
    }
}
