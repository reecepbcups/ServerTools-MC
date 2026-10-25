package sh.reece.core.teleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Schedulers;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import static sh.reece.utiltools.TextUtil.format;

/**
 * /rtp [world] and /rtp &lt;player&gt; [world]. Only worlds listed under Worlds can be used. Chunks load async and
 * each spot is checked on its own region thread, so a search never stalls the server.
 */
final class RandomTeleportCommand extends BaseCommand {

    static final String SECTION = "Core.RandomTeleport";

    private static final Logger LOGGER = LoggerFactory.getLogger(RandomTeleportCommand.class);

    private static final String NO_WORLD_ACCESS = "<red>You can't random teleport in <white>%s</white>.";
    private static final String NOT_ENABLED = "<red>Random teleport isn't set up for <white>%s</white>.";
    private static final String NOT_ONLINE = "<red>Player <white>%s</white> isn't online.";
    private static final String USAGE_CONSOLE = "Usage: /rtp \\<player> [world]";
    private static final String SEARCHING = "<gray>Looking for a safe spot...";
    private static final String SENDING = "<green>Random teleporting <white>%s</white>.";

    /** Where in a world to search, a ring between min and max radius around the center. */
    record Area(int centerX, int centerZ, int minRadius, int maxRadius) {}

    private final Teleporter teleporter;
    private final TeleportAction action;
    private final String othersPerm;
    private final String worldPermPrefix;
    private final String defaultWorld;
    private final int maxAttempts;
    private final Map<String, Area> areas = new LinkedHashMap<>();
    private final Set<Material> unsafe = EnumSet.noneOf(Material.class);

    RandomTeleportCommand(final Main plugin, final Teleporter teleporter) {
        super(plugin, SECTION, "rtp");
        this.teleporter = teleporter;
        final FileConfiguration config = plugin.getConfig();
        this.action = TeleportAction.fromConfig(config, SECTION);
        this.othersPerm = config.getString(SECTION + ".OthersPerm", "tools.rtp.others");
        this.worldPermPrefix = config.getString(SECTION + ".WorldPermPrefix", "tools.rtp.world.");
        this.defaultWorld = config.getString(SECTION + ".DefaultWorld", "world");
        this.maxAttempts = Math.max(1, config.getInt(SECTION + ".MaxAttempts", 10));

        final ConfigurationSection worlds = config.getConfigurationSection(SECTION + ".Worlds");
        if (worlds != null) {
            for (final String name : worlds.getKeys(false)) {
                final ConfigurationSection w = worlds.getConfigurationSection(name);
                if (w == null) continue;
                final int min = Math.max(0, w.getInt("MinRadius", 0));
                final int max = Math.max(min + 1, w.getInt("MaxRadius", 5000));
                this.areas.put(name, new Area(w.getInt("CenterX", 0), w.getInt("CenterZ", 0), min, max));
            }
        }
        for (final String name : config.getStringList(SECTION + ".UnsafeBlocks")) {
            final Material material = Material.matchMaterial(name);
            if (material == null) {
                LOGGER.warn("Unknown block {} in {}.UnsafeBlocks", name, SECTION);
            } else {
                this.unsafe.add(material);
            }
        }
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        if (noPermission(sender, cmd)) {
            return true;
        }

        // first arg is a world if one by that name is set up, else a player for staff
        Player target = sender instanceof Player p ? p : null;
        String worldArg = null;
        if (args.length > 0) {
            if (this.areas.containsKey(args[0])) {
                worldArg = args[0];
            } else if (hasPermission(sender, this.othersPerm)) {
                target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage(format(NOT_ONLINE, args[0]));
                    return true;
                }
                worldArg = args.length > 1 ? args[1] : null;
            } else {
                sender.sendMessage(format(NOT_ENABLED, args[0]));
                return true;
            }
        }
        if (target == null) {
            sender.sendMessage(format(USAGE_CONSOLE));
            return true;
        }
        final boolean self = target == sender;

        final String worldName = worldArg != null ? worldArg
                : this.areas.containsKey(target.getWorld().getName()) ? target.getWorld().getName() : this.defaultWorld;
        final Area area = this.areas.get(worldName);
        final World world = Bukkit.getWorld(worldName);
        if (area == null || world == null) {
            sender.sendMessage(format(NOT_ENABLED, worldName));
            return true;
        }
        if (self && worldArg != null && !hasPermission(sender, this.worldPermPrefix + worldName)) {
            sender.sendMessage(format(NO_WORLD_ACCESS, worldName));
            return true;
        }
        if (!self) {
            sender.sendMessage(format(SENDING, target.getName()));
        }

        final Player mover = target;
        this.teleporter.teleport(mover, mover, self ? this.action : TeleportAction.INSTANT, () -> {
            mover.sendMessage(format(SEARCHING));
            return find(world, area, this.maxAttempts);
        }, "a random spot");
        return true;
    }

    /** Completes with null when every attempt came up unsafe. */
    private CompletableFuture<Location> find(final World world, final Area area, final int attemptsLeft) {
        if (attemptsLeft <= 0) {
            return CompletableFuture.completedFuture(null);
        }

        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final double angle = random.nextDouble(Math.PI * 2);
        final double radius = random.nextDouble(area.minRadius(), area.maxRadius());
        final int x = area.centerX() + (int) Math.round(Math.cos(angle) * radius);
        final int z = area.centerZ() + (int) Math.round(Math.sin(angle) * radius);
        final Location column = new Location(world, x, 0, z);
        if (!world.getWorldBorder().isInside(column)) {
            return find(world, area, attemptsLeft - 1);
        }

        return world.getChunkAtAsync(x >> 4, z >> 4).thenCompose(chunk -> {
            final CompletableFuture<Location> checked = new CompletableFuture<>();
            Schedulers.region(this.plugin, column, () -> checked.complete(safeSpot(world, x, z)));
            return checked;
        }).thenCompose(found -> found != null
                ? CompletableFuture.completedFuture(found)
                : find(world, area, attemptsLeft - 1));
    }

    /** Standing spot at x,z or null. Nether scans down under the roof, other worlds only try the surface. */
    private Location safeSpot(final World world, final int x, final int z) {
        final boolean nether = world.getEnvironment() == World.Environment.NETHER;
        final int top = nether ? Math.min(world.getMaxHeight(), 127) - 2 : world.getHighestBlockYAt(x, z);
        final int bottom = nether ? world.getMinHeight() + 1 : top;

        for (int y = top; y >= bottom; y--) {
            final Block floor = world.getBlockAt(x, y, z);
            if (!floor.getType().isSolid() || this.unsafe.contains(floor.getType())) continue;
            if (clear(floor.getRelative(0, 1, 0)) && clear(floor.getRelative(0, 2, 0))) {
                return new Location(world, x + 0.5, y + 1, z + 0.5);
            }
        }
        return null;
    }

    private boolean clear(final Block block) {
        return block.isPassable() && !block.isLiquid() && !this.unsafe.contains(block.getType());
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        if (args.length > 2) return List.of();
        final String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        final List<String> options = new ArrayList<>();
        for (final String world : this.areas.keySet()) {
            if (world.toLowerCase(Locale.ROOT).startsWith(prefix)) options.add(world);
        }
        if (args.length == 1 && hasPermission(sender, this.othersPerm)) {
            options.addAll(super.onTabComplete(sender, cmd, label, args));
        }
        return options;
    }
}
