package sh.reece.core.hologram;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.TextUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static net.kyori.adventure.text.Component.text;

/**
 * The {@code /hologram} command surface; state and rendering live in {@link HologramFeature}.
 *
 * <p>The two-argument constructor is deliberate: {@code Loader} only instantiates classes with a
 * lone {@code (Main)} constructor, so it skips this one and {@link HologramFeature} builds it
 * instead. That keeps the two halves paired.
 */
public final class HologramCommand extends BaseCommand {

    private static final String FALLBACK_PERMISSION = "hologram.admin";

    private static final double REMOVE_NEAR_RADIUS = 2.0D;

    private static final List<String> SUB_COMMANDS =
            List.of("create", "set", "remove", "show", "hide", "list", "teleport", "removenear", "reload");

    private static final List<String> KEYED_SUB_COMMANDS =
            List.of("set", "remove", "delete", "show", "hide", "teleport", "tp");

    private static final Map<String, List<String>> SET_OPTIONS = Map.of(
            Hologram.BILLBOARD_KEY, List.of("center", "fixed", "vertical", "horizontal"),
            Hologram.YAW_KEY, List.of("here", "0", "90", "180", "-90"),
            Hologram.PITCH_KEY, List.of("here", "0"),
            Hologram.BACKGROUND_KEY, List.of("default", "clear", "#80000000", "#CC000000"),
            Hologram.SEE_THROUGH_KEY, List.of("true", "false"),
            Hologram.SHADOW_KEY, List.of("true", "false"),
            Hologram.UPDATE_INTERVAL_KEY, List.of("0", "20", "100", "1200"));

    private final HologramFeature feature;
    private final String adminPermission;

    HologramCommand(final Main plugin, final HologramFeature feature) {
        super(plugin, HologramFeature.SECTION, "hologram", "holograms", "holo", "holos");
        this.feature = feature;

        // BaseCommand leaves permission empty when unset, and empty means everyone
        final String configured = plugin.getConfig().getString(HologramFeature.SECTION + ".Permission");
        this.adminPermission = (configured == null || configured.isBlank())
                ? FALLBACK_PERMISSION : configured;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        // console is always trusted; players need the permission
        if (sender instanceof Player && !sender.isOp() && !sender.hasPermission(this.adminPermission)) {
            send(sender, "<red>You do not have access to <white>/" + label + "<red>.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        final String key = args.length >= 2 ? args[1] : null;

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> create(sender, key);
            case "set" -> set(sender, key, args.length >= 3 ? args[2] : null, args.length >= 4 ? args[3] : null);
            case "remove", "delete" -> remove(sender, key);
            case "show" -> show(sender, key);
            case "hide" -> hide(sender, key);
            case "teleport", "tp" -> teleport(sender, key);
            case "list" -> list(sender);
            case "removenear" -> removeNear(sender);
            case "reload" -> {
                this.feature.load();
                send(sender, "<green>Reloaded " + this.feature.holograms().size() + " hologram(s).");
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void create(final CommandSender sender, final String key) {
        final Player player = playerOrNull(sender);
        if (player == null) {
            return;
        }
        if (key == null) {
            sendHelp(sender);
            return;
        }

        final FileConfiguration config = config();
        if (config.contains(key)) {
            send(sender, "<red>A hologram named <white>", key, "<red> already exists.");
            return;
        }

        // Y_OFFSET back on so the render-time subtraction lands it at the player's feet
        final Location at = player.getLocation().clone().add(0, Hologram.Y_OFFSET, 0);
        config.set(key + "." + Hologram.LOCATION_KEY, formatLocation(at));
        config.set(key + "." + Hologram.TEXT_KEY,
                List.of("<white>Edit this line in", "<aqua>the " + HologramFeature.CONFIG_FILE));
        this.configUtils.saveConfig(config, HologramFeature.CONFIG_FILE);

        this.feature.load();
        send(sender, "<green>Created <white>", key, "<green> at your location.");
    }

    private void set(final CommandSender sender, final String key, final String option, final String value) {
        if (key == null || option == null || value == null || !SET_OPTIONS.containsKey(option.toLowerCase(Locale.ROOT))) {
            send(sender, "<white>/hologram <gray>set <name> <" + String.join("|", SET_OPTIONS.keySet().stream().sorted().toList()) + "> <value>");
            return;
        }

        final FileConfiguration config = config();
        if (!config.contains(key)) {
            unknown(sender, key);
            return;
        }

        final String path = key + "." + option.toLowerCase(Locale.ROOT);
        final Object previous = config.get(path);
        final Object parsed;
        try {
            parsed = setValue(sender, option.toLowerCase(Locale.ROOT), value);
            config.set(path, parsed);
            // parse is the source of truth for what a valid entry looks like
            Hologram.parse(key, config.getConfigurationSection(key));
        } catch (final RuntimeException e) {
            config.set(path, previous);
            send(sender, "<red>Invalid value <white>", value, "<red> for " + option + ".");
            return;
        }

        this.configUtils.saveConfig(config, HologramFeature.CONFIG_FILE);
        this.feature.load();
        send(sender, "<green>Set <white>", key, "<green> " + option + " to <white>" + parsed + "<green>.");
    }

    /** {@code here} on yaw or pitch takes the player's facing, so a fixed board can face where they stand. */
    private static Object setValue(final CommandSender sender, final String option, final String value) {
        return switch (option) {
            case Hologram.YAW_KEY, Hologram.PITCH_KEY -> {
                if (value.equalsIgnoreCase("here") && sender instanceof Player player) {
                    final Location facing = player.getLocation();
                    // the text's front faces back along the player's view
                    yield (double) Math.round(option.equals(Hologram.YAW_KEY)
                            ? Location.normalizeYaw(facing.getYaw() + 180F) : -facing.getPitch());
                }
                yield Double.parseDouble(value);
            }
            case Hologram.SEE_THROUGH_KEY, Hologram.SHADOW_KEY -> {
                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    throw new IllegalArgumentException(value);
                }
                yield Boolean.parseBoolean(value);
            }
            case Hologram.UPDATE_INTERVAL_KEY -> Long.parseLong(value);
            default -> value.toLowerCase(Locale.ROOT);
        };
    }

    private void remove(final CommandSender sender, final String key) {
        if (key == null) {
            sendHelp(sender);
            return;
        }

        final FileConfiguration config = config();
        if (!config.contains(key)) {
            unknown(sender, key);
            return;
        }

        // load() below takes the displays down: it still holds this hologram, and the
        // key is about to be gone from the config it re-reads
        config.set(key, null);
        this.configUtils.saveConfig(config, HologramFeature.CONFIG_FILE);

        this.feature.load();
        send(sender, "<green>Removed <white>", key, "<green>.");
    }

    private void show(final CommandSender sender, final String key) {
        if (key == null) {
            sendHelp(sender);
            return;
        }

        final Optional<Hologram> hologram = this.feature.hologram(key);
        if (hologram.isEmpty()) {
            unknown(sender, key);
            return;
        }

        hologram.get().spawn(this.plugin);
        send(sender, "<green>Showing <white>", key, "<green>.");
    }

    private void hide(final CommandSender sender, final String key) {
        if (key == null) {
            sendHelp(sender);
            return;
        }

        final Optional<Hologram> hologram = this.feature.hologram(key);
        if (hologram.isEmpty()) {
            unknown(sender, key);
            return;
        }

        hologram.get().despawn(this.plugin);
        send(sender, "<green>Hid <white>", key, "<green>.");
    }

    private void teleport(final CommandSender sender, final String key) {
        final Player player = playerOrNull(sender);
        if (player == null) {
            return;
        }

        if (key == null) {
            sendHelp(sender);
            return;
        }

        final Optional<Hologram> hologram = this.feature.hologram(key);
        if (hologram.isEmpty()) {
            unknown(sender, key);
            return;
        }

        final Location destination = configLocation(hologram.get());
        if (destination.getWorld() == null) {
            send(sender, "<red>The world <white>", key, "<red> lives in is not loaded.");
            return;
        }

        player.teleportAsync(destination);
        send(sender, "<green>Teleported to <white>", key, "<green>.");
    }

    private void list(final CommandSender sender) {
        final List<Hologram> holograms = this.feature.holograms();
        send(sender, "<yellow><bold>SERVERTOOLS HOLOGRAMS</bold> <gray>(" + holograms.size() + ")");
        if (holograms.isEmpty()) {
            send(sender, "<gray>Nothing loaded from " + HologramFeature.CONFIG_FILE
                    + " - try <white>/hologram create <name></white>.");
            return;
        }

        for (final Hologram hologram : holograms) {
            sender.sendMessage(TextUtil.color("<gray>- ")
                    .append(text(hologram.key()))
                    .append(TextUtil.color("<gray>: <white>" + formatLocation(configLocation(hologram)))));
        }
    }

    private void removeNear(final CommandSender sender) {
        final Player player = playerOrNull(sender);
        if (player == null) {
            return;
        }

        player.getScheduler().execute(this.plugin, () -> {
            int removed = 0;
            for (final Entity entity : player.getNearbyEntities(REMOVE_NEAR_RADIUS, REMOVE_NEAR_RADIUS, REMOVE_NEAR_RADIUS)) {
                if (entity instanceof TextDisplay
                        || (entity instanceof ArmorStand stand && stand.isCustomNameVisible())) {
                    entity.remove();
                    removed++;
                }
            }

            send(sender, "<green>Removed " + removed + " display(s) within "
                    + (int) REMOVE_NEAR_RADIUS + " blocks.");
        }, null, 1L);
    }

    private void sendHelp(final CommandSender sender) {
        send(sender, "<yellow><bold>ServerTools Holograms");
        for (final String usage : new String[] {
                "create <name>", "set <name> <option> <value>", "remove <name>", "show <name>", "hide <name>",
                "teleport <name>", "list", "removenear", "reload" }) {
            send(sender, "<white>/hologram <gray>" + usage);
        }
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        if (args.length == 1) {
            return startingWith(SUB_COMMANDS, args[0]);
        }

        if (args.length == 2) {
            if (KEYED_SUB_COMMANDS.contains(args[0].toLowerCase(Locale.ROOT))) {
                return startingWith(this.feature.holograms().stream().map(Hologram::key).toList(), args[1]);
            }
            if (args[0].equalsIgnoreCase("create")) {
                return List.of("<name>");
            }
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (args.length == 3) {
                return startingWith(SET_OPTIONS.keySet().stream().sorted().toList(), args[2]);
            }
            if (args.length == 4) {
                return startingWith(SET_OPTIONS.getOrDefault(args[2].toLowerCase(Locale.ROOT), List.of()), args[3]);
            }
        }

        return List.of();
    }

    private FileConfiguration config() {
        return this.configUtils.getConfigFile(HologramFeature.CONFIG_FILE);
    }

    /** The location as it appears in the config, i.e. with the render offset added back on. */
    private static Location configLocation(final Hologram hologram) {
        return hologram.location().add(0, Hologram.Y_OFFSET, 0);
    }

    /** Matches the {@code world, x, y, z} format Holograms.yml has always used. */
    private static String formatLocation(final Location location) {
        // an entry naming a world the server never loaded still has to list, so the name
        // is a placeholder rather than an NPE - only create() writes this back, and its
        // location comes from a player, so it always has a world
        final World world = location.getWorld();
        // Locale.ROOT: a ',' decimal separator would write a string parse cannot split back apart
        return String.format(Locale.ROOT, "%s, %.3f, %.3f, %.3f",
                world == null ? "unknown" : world.getName(),
                location.getX(), location.getY(), location.getZ());
    }

    private static List<String> startingWith(final Iterable<String> candidates, final String prefix) {
        final String lower = prefix.toLowerCase(Locale.ROOT);
        final List<String> matches = new ArrayList<>();
        for (final String candidate : candidates) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(lower)) {
                matches.add(candidate);
            }
        }
        return matches;
    }

    private static void unknown(final CommandSender to, final String key) {
        send(to, "<red>No hologram named <white>", key, "<red>.");
    }

    private static void send(final CommandSender to, final String message) {
        to.sendMessage(TextUtil.color(message));
    }

    /** The literal half is never parsed, so a key containing markup cannot inject tags. */
    private static void send(final CommandSender to, final String before, final String literal, final String after) {
        to.sendMessage(TextUtil.color(before)
                .append(text(literal))
                .append(TextUtil.color(after)));
    }
}
