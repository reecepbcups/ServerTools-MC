package sh.reece.core.warp.v2;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.TextUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * The {@code /warp} command surface; the warps themselves live in {@link WarpFeature}.
 */
public final class WarpComand extends BaseCommand {

    private static final String NO_PERMISSION = "<red>You do not have permission to use this command.";
    private static final String NO_PERMISSION_OTHERS = "<red>You do not have permission to teleport other players.";
    private static final String NO_ACCESS = "<red>You do not have access to '%s'. <italic>(%s)";

    private static final String USAGE_SETWARP = "Usage: /setwarp \\<warp> [permission]";
    private static final String USAGE_DELWARP = "Usage: /delwarp \\<warp>";
    private static final String USAGE_WARPINFO = "Usage: /warpinfo \\<warp>";

    private static final String NOT_A_WARP = "<red>This is not a warp location.";
    private static final String WARP_NOT_FOUND = "<red>Warp '%s' not found.";
    private static final String WARP_DOES_NOT_EXIST = "<red>This warp does not exist!";
    private static final String WARP_ALREADY_SET = "<red>There is already a warp set as this name!";
    private static final String WARP_NAME_NUMERIC = "<red>You can not set a warp as a number.";
    private static final String WARP_NAME_DOTTED = "<red>You can not set a warp with a '.' in the name.";

    private static final String WARP_SET = "<green>Warp set as <underlined>%s</underlined><green>.";
    private static final String WARP_DELETED = "<green>Warp <underlined>%s</underlined><green> has been deleted.";

    private static final String PLAYER_NOT_ONLINE = "<red>Player '%s' not found online to warp to that location.";
    private static final String TELEPORTING_OTHER = "<green>Teleporting <white>%s <green>to warp <white>%s<gray>.";
    private static final String TELEPORTED = "<dark_green>[!] <green>Teleported to <underlined>%s</underlined><dark_green>.";
    private static final String TELEPORTED_BY = TELEPORTED + " <gray><italic>(( from %s ))";

    private static final String LIST_HEADER = "\n<white><bold>Warps</bold> <gray><italic>( %s )";
    private static final String LIST_ENTRY = "<white>%s<gray>, ";
    private static final String LIST_ENTRY_LOCKED = "<red>%s<gray>, ";

    private static final String INFO_NAME = "<white><bold>Warp</bold><gray>: <yellow>%s";
    private static final String INFO_PERMISSION = "<white><bold>Permission</bold><gray>: <yellow>%s";
    private static final String INFO_LOCATION = "<white><bold>Location</bold><gray>: <yellow>%s %s %s %s";

    private final WarpFeature feature;

    WarpComand(final Main plugin, final WarpFeature feature) {
        super(plugin, WarpConfiguration.SECTION,
                "warp", "warps", "setwarp", "addwarp", "delwarp", "remwarp", "warpinfo");
        this.feature = feature;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        switch (label.toLowerCase(Locale.ROOT)) {
            case "setwarp", "addwarp" -> set(sender, args);
            case "delwarp", "remwarp" -> delete(sender, args);
            case "warpinfo" -> info(sender, args);
            case "warps" -> list(sender);
            default -> teleport(sender, args);
        }
        return true;
    }

    private void teleport(final CommandSender sender, final String[] args) {
        if (args.length == 0 || isInteger(args[0])) {
            list(sender);
            return;
        }

        final Optional<Warp> found = this.feature.warp(args[0]);
        if (found.isEmpty()) {
            send(sender, NOT_A_WARP);
            return;
        }
        final Warp warp = found.get();

        if (args.length == 1) {
            final Player self = playerOrNull(sender);
            if (self == null) {
                return;
            }
            if (!canUse(self, warp)) {
                send(sender, NO_ACCESS, warp.name(), warp.permission());
                return;
            }
            arrive(self, warp, null);
            return;
        }

        if (!allows(sender, this.feature.configuration()::canWarpOthers)) {
            send(sender, NO_PERMISSION_OTHERS);
            return;
        }

        final Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            send(sender, PLAYER_NOT_ONLINE, args[1]);
            return;
        }

        send(sender, TELEPORTING_OTHER, target.getName(), warp.name());
        arrive(target, warp, sender.getName());
    }

    private void list(final CommandSender sender) {
        final var warps = this.feature.warps();
        send(sender, LIST_HEADER, warps.size());

        Component line = Component.empty();
        for (final Warp warp : warps) {
            final boolean usable = !(sender instanceof Player player) || canUse(player, warp);
            line = line.append(TextUtil.format(usable ? LIST_ENTRY : LIST_ENTRY_LOCKED, warp.name()));
        }
        sender.sendMessage(line);
    }

    private void set(final CommandSender sender, final String[] args) {
        if (!allows(sender, this.feature.configuration()::canSetWarp)) {
            send(sender, NO_PERMISSION);
            return;
        }

        final Player player = playerOrNull(sender);
        if (player == null) {
            return;
        }
        if (args.length == 0) {
            send(sender, USAGE_SETWARP);
            return;
        }

        final String name = args[0];
        if (isInteger(name)) {
            send(sender, WARP_NAME_NUMERIC);
            return;
        }
        if (name.indexOf('.') >= 0) {
            send(sender, WARP_NAME_DOTTED);
            return;
        }
        if (this.feature.warp(name).isPresent()) {
            send(sender, WARP_ALREADY_SET);
            return;
        }

        final String permission = args.length >= 2 ? args[1] : "";
        Warp.save(warps(), new Warp(name, permission, player.getLocation()), true);
        this.feature.load();

        send(sender, WARP_SET, name);
    }

    private void delete(final CommandSender sender, final String[] args) {
        if (!allows(sender, this.feature.configuration()::canDeleteWarp)) {
            send(sender, NO_PERMISSION);
            return;
        }
        if (args.length == 0) {
            send(sender, USAGE_DELWARP);
            return;
        }

        final Optional<Warp> warp = this.feature.warp(args[0]);
        if (warp.isEmpty()) {
            send(sender, WARP_DOES_NOT_EXIST);
            return;
        }

        Warp.delete(warps(), warp.get(), true);
        this.feature.load();

        send(sender, WARP_DELETED, args[0]);
    }

    private void info(final CommandSender sender, final String[] args) {
        if (!allows(sender, this.feature.configuration()::canViewWarps)) {
            send(sender, NO_PERMISSION);
            return;
        }
        if (args.length == 0) {
            send(sender, USAGE_WARPINFO);
            return;
        }

        final Optional<Warp> found = this.feature.warp(args[0]);
        if (found.isEmpty()) {
            send(sender, WARP_NOT_FOUND, args[0]);
            return;
        }
        final Warp warp = found.get();
        final Location location = warp.location();
        final World world = location.getWorld();

        send(sender, INFO_NAME, warp.name());
        if (!warp.permission().isBlank()) {
            send(sender, INFO_PERMISSION, warp.permission());
        }
        send(sender, INFO_LOCATION, world == null ? "unknown" : world.getName(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        final String action = label.toLowerCase(Locale.ROOT);
        final boolean naming = action.equals("setwarp") || action.equals("addwarp");

        if (args.length == 1) {
            return naming ? List.of("<warp-name>") : startingWith(visibleTo(sender), args[0]);
        }
        if (args.length == 2 && naming) {
            return List.of("[permission]");
        }
        return super.onTabComplete(sender, cmd, label, args);
    }

    private List<String> visibleTo(final CommandSender sender) {
        final List<String> names = new ArrayList<>();
        for (final Warp warp : this.feature.warps()) {
            if (!(sender instanceof Player player) || canUse(player, warp)) {
                names.add(warp.name());
            }
        }
        return names;
    }

    private FileConfiguration warps() {
        return this.configUtils.getConfigFile(Warp.WARPS_FILE);
    }

    private void arrive(final Player player, final Warp warp, final String sentBy) {
        player.teleportAsync(warp.location()).thenRun(() -> {
            if (sentBy == null) {
                send(player, TELEPORTED, warp.name());
            } else {
                send(player, TELEPORTED_BY, warp.name(), sentBy);
            }
        });
    }

    private static boolean canUse(final Player player, final Warp warp) {
        return warp.permission().isBlank() || player.hasPermission(warp.permission());
    }

    private static boolean allows(final CommandSender sender, final Predicate<Player> node) {
        return !(sender instanceof Player player) || node.test(player);
    }

    private static boolean isInteger(final String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (final NumberFormatException e) {
            return false;
        }
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

    private static void send(final CommandSender to, final String template, final Object... placeholders) {
        to.sendMessage(TextUtil.format(template, placeholders));
    }
}
