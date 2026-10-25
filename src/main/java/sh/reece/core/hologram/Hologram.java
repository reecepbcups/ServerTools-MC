package sh.reece.core.hologram;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Color;
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
import sh.reece.core.economy.Money;
import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.utiltools.TextUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.kyori.adventure.text.Component.text;

/**
 * One hologram from {@code Holograms.yml}, and the displays that draw it.
 */
public record Hologram(String key, List<String> lines, Location location, Appearance appearance, long updateInterval) {

    public static final String LOCATION_KEY = "location";
    public static final String TEXT_KEY = "lines";
    public static final String BILLBOARD_KEY = "billboard";
    public static final String YAW_KEY = "yaw";
    public static final String PITCH_KEY = "pitch";
    public static final String BACKGROUND_KEY = "background";
    public static final String SEE_THROUGH_KEY = "see-through";
    public static final String SHADOW_KEY = "shadow";
    public static final String UPDATE_INTERVAL_KEY = "update-interval";
    public static final NamespacedKey IDENTIFIER_KEY = new NamespacedKey("servertools", "hologram");
    private static final NamespacedKey LINE_KEY = new NamespacedKey("servertools", "hologram_line");

    // %baltop_<rank>_name% and %baltop_<rank>_balance%, ranks start at 1
    private static final Pattern BALTOP = Pattern.compile("%baltop_(\\d{1,3})_(name|balance)%");

    /**
     * How each line's display looks. A null background keeps the vanilla grey box.
     */
    public record Appearance(Display.Billboard billboard, float yaw, float pitch, Color background,
                             boolean seeThrough, boolean shadow) {
        public static final Appearance DEFAULT =
                new Appearance(Display.Billboard.CENTER, 0F, 0F, null, false, false);
    }

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
    public boolean spawn(final Main plugin) {
        return spawn(plugin, false);
    }

    /**
     * @param sweepLegacy also removes untagged displays drawn by the pre-rewrite class; done once on the
     *                    first load after the version bump, see {@link HologramFeature#load()}
     */
    public boolean spawn(final Main plugin, final boolean sweepLegacy) {
        final World world = this.location.getWorld();
        if (world == null) {
            return false;
        }

        loadLocationAndExecute(plugin, () -> {
            erase(world);
            if (sweepLegacy) {
                eraseLegacy(world);
            }
            draw(world, render(plugin, this.lines));
        });
        return true;
    }

    /**
     * Removes only displays tagged with {@link #IDENTIFIER_KEY} for this key.
     */
    public void despawn(final Main plugin) {
        final World world = this.location.getWorld();
        if (world == null) {
            return;
        }

        loadLocationAndExecute(plugin, () -> erase(world));
    }

    /**
     * Re-renders the lines into the displays already up, so placeholders stay live without respawning.
     *
     * @return null when {@code update-interval} is off
     */
    public ScheduledTask scheduleRefresh(final Main plugin) {
        if (this.updateInterval <= 0 || this.location.getWorld() == null) {
            return null;
        }

        // unloaded chunk means nobody is looking, so finding no displays is fine
        return Bukkit.getRegionScheduler().runAtFixedRate(plugin, this.location,
                (task) -> refresh(this.location.getWorld(), render(plugin, this.lines)),
                this.updateInterval, this.updateInterval);
    }

    private void draw(final World world, final List<Component> rendered) {
        Location current = this.location();
        current.setYaw(this.appearance.yaw());
        current.setPitch(this.appearance.pitch());
        for (int i = 0; i < this.lines.size(); i++) {
            current = current.clone().subtract(0, LINE_SPACING, 0);

            // a blank line is a gap: it moves the cursor down but gets no entity. Judged on the raw
            // line so a placeholder that resolves empty keeps its slot for refresh
            if (this.lines.get(i).isEmpty()) {
                continue;
            }

            final int index = i;
            world.spawn(current, TextDisplay.class, (display) -> {
                display.setBillboard(this.appearance.billboard());
                if (this.appearance.background() != null) {
                    display.setDefaultBackground(false);
                    display.setBackgroundColor(this.appearance.background());
                }
                display.setSeeThrough(this.appearance.seeThrough());
                display.setShadowed(this.appearance.shadow());
                display.text(rendered.get(index));
                display.getPersistentDataContainer().set(IDENTIFIER_KEY, PersistentDataType.STRING, this.key);
                display.getPersistentDataContainer().set(LINE_KEY, PersistentDataType.INTEGER, index);
            });
        }
    }

    private void refresh(final World world, final List<Component> rendered) {
        for (final TextDisplay display : tagged(world)) {
            final Integer index = display.getPersistentDataContainer().get(LINE_KEY, PersistentDataType.INTEGER);
            if (index == null || index >= rendered.size()) {
                continue;
            }

            // skip unchanged text so idle boards send no metadata packets
            final Component line = rendered.get(index);
            if (!line.equals(display.text())) {
                display.text(line);
            }
        }
    }

    private Collection<TextDisplay> tagged(final World world) {
        return world.getNearbyEntitiesByType(TextDisplay.class, this.location, SWEEP_XZ, SWEEP_Y, (display) -> {
            final PersistentDataContainer container = display.getPersistentDataContainer();
            return this.key.equals(container.get(IDENTIFIER_KEY, PersistentDataType.STRING));
        });
    }

    private void erase(final World world) {
        tagged(world).forEach(Entity::remove);
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

    private void loadLocationAndExecute(final Main plugin, final Runnable runnable) {
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

        final Appearance appearance = new Appearance(
                Display.Billboard.valueOf(section.getString(BILLBOARD_KEY, "center").trim().toUpperCase(Locale.ROOT)),
                (float) section.getDouble(YAW_KEY, 0D),
                (float) section.getDouble(PITCH_KEY, 0D),
                parseColor(section.getString(BACKGROUND_KEY, "default")),
                section.getBoolean(SEE_THROUGH_KEY, false),
                section.getBoolean(SHADOW_KEY, false));

        return new Hologram(key, section.getStringList(TEXT_KEY), location, appearance,
                section.getLong(UPDATE_INTERVAL_KEY, 0L));
    }

    /**
     * {@code default}, {@code clear} or {@code #AARRGGBB}; {@code #RRGGBB} is taken as fully opaque.
     */
    public static Color parseColor(final String value) {
        final String hex = value.trim().replace("#", "");
        if (hex.equalsIgnoreCase("default")) {
            return null;
        }
        if (hex.equalsIgnoreCase("clear")) {
            return Color.fromARGB(0);
        }
        if (hex.length() != 6 && hex.length() != 8) {
            throw new IllegalArgumentException("background must be 'default', 'clear', #RRGGBB or #AARRGGBB, got " + value);
        }

        final long argb = Long.parseLong(hex, 16);
        return Color.fromARGB((int) (hex.length() == 6 ? argb | 0xFF000000L : argb));
    }

    static void warn(final String key, final String problem) {
        TextUtil.consoleMessage(TextUtil.color("<red>[!] Hologram <white>").append(text(key)).append(TextUtil.color("<red> ")).append(text(problem)));
    }

    private static List<Component> render(final Main plugin, final List<String> lines) {
        // one sort per render, not one per placeholder
        final Map<UUID, Long> top = needsBalTop(lines) && plugin.getEconomyStorage() != null
                ? plugin.getEconomyStorage().top(maxRank(lines)) : Map.of();
        final List<UUID> ranked = List.copyOf(top.keySet());

        return lines.stream().map((line) -> render(replaceBalTop(line, ranked, top, plugin.getCurrencySymbol()))).toList();
    }

    private static boolean needsBalTop(final List<String> lines) {
        return lines.stream().anyMatch((line) -> BALTOP.matcher(line).find());
    }

    private static int maxRank(final List<String> lines) {
        int max = 0;
        for (final String line : lines) {
            final Matcher matcher = BALTOP.matcher(line);
            while (matcher.find()) {
                max = Math.max(max, Integer.parseInt(matcher.group(1)));
            }
        }
        return max;
    }

    private static String replaceBalTop(final String line, final List<UUID> ranked, final Map<UUID, Long> top,
                                        final String symbol) {
        if (!line.contains("%baltop_")) {
            return line;
        }

        return BALTOP.matcher(line).replaceAll((match) -> {
            final int rank = Integer.parseInt(match.group(1));
            if (rank < 1 || rank > ranked.size()) {
                return match.group(2).equals("name") ? "---" : "";
            }

            final UUID id = ranked.get(rank - 1);
            if (match.group(2).equals("balance")) {
                return Matcher.quoteReplacement(Money.format(top.get(id), symbol));
            }
            final String name = Bukkit.getOfflinePlayer(id).getName();
            return Matcher.quoteReplacement(name == null ? id.toString() : name);
        });
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
