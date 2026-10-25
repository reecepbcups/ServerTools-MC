package sh.reece.core.teleport;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Schedulers;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import static sh.reece.utiltools.TextUtil.format;

/**
 * /home, /homes, /sethome and /delhome. Staff with the others perm can reach anyone's homes as {@code player:name}.
 */
final class HomeCommand extends BaseCommand {

    static final String SECTION = "Core.Homes";

    private static final String NO_PERMISSION = "<red>You do not have permission to use this command.";
    private static final String BAD_NAME = "<red>Home names are 1-16 letters, numbers, _ or -.";
    private static final String DISABLED_WORLD = "<red>You can't set homes in this world.";
    private static final String AT_LIMIT = "<red>You can only have <white>%s</white> homes. Delete one with /delhome.";
    private static final String SET = "<green>Home <white>%s</white> set.";
    private static final String DELETED = "<green>Home <white>%s</white> deleted.";
    private static final String NOT_FOUND = "<red>No home named <white>%s</white>.";
    private static final String NO_HOMES = "<red>No homes yet. Set one with /sethome.";
    private static final String UNKNOWN_PLAYER = "<red>Player <white>%s</white> has never joined.";
    private static final String WORLD_GONE = "<red>The world <white>%s</white> for that home isn't loaded.";
    private static final String USAGE_DELHOME = "Usage: /delhome \\<name>";
    private static final String LIST_HEADER = "<white><bold>Homes</bold> <gray>(%s/%s)";
    private static final String LIST_HEADER_OTHER = "<white><bold>%s's homes</bold> <gray>(%s)";
    private static final String LIST_ENTRY = "<hover:show_text:'<gray>Click to teleport'><click:run_command:'/home %1$s'><white>%1$s</white></click></hover><gray>, ";

    private final Teleporter teleporter;
    private final HomeData data;
    private final TeleportAction action;
    private final String othersPerm;
    private final String limitPermPrefix;
    private final Map<String, Integer> limits = new LinkedHashMap<>();
    private final List<String> disabledWorlds;
    private ScheduledTask saveTask;

    HomeCommand(final Main plugin, final Teleporter teleporter) {
        super(plugin, SECTION, "home", "homes", "sethome", "delhome");
        this.teleporter = teleporter;
        final FileConfiguration config = plugin.getConfig();
        this.data = new HomeData(new File(plugin.getDataFolder(), HomeData.FILE));
        this.action = TeleportAction.fromConfig(config, SECTION);
        this.othersPerm = config.getString(SECTION + ".OthersPerm", "tools.home.others");
        this.limitPermPrefix = config.getString(SECTION + ".LimitPermPrefix", "tools.homes.limit.");
        this.disabledWorlds = config.getStringList(SECTION + ".DisabledWorlds");
        final ConfigurationSection tiers = config.getConfigurationSection(SECTION + ".Limits");
        if (tiers != null) {
            for (final String tier : tiers.getKeys(false)) {
                this.limits.put(tier, tiers.getInt(tier));
            }
        }
        if (!isEnabled()) {
            return;
        }

        this.data.load();
        final long period = 20L * 60 * Math.max(1, config.getInt(SECTION + ".SaveIntervalMinutes", 5));
        this.saveTask = Schedulers.globalTimer(plugin,
                () -> Schedulers.async(plugin, this.data::saveIfDirty), period, period);
    }

    /** Flushes pending changes. Runs on the unload thread so nothing is lost on shutdown. */
    void shutdown() {
        if (this.saveTask != null) {
            this.saveTask.cancel();
        }
        if (isEnabled()) {
            this.data.saveIfDirty();
        }
    }

    /**
     * Highest tier the player holds wins. {@code default} applies to everyone, any negative tier means unlimited.
     */
    static int limit(final Map<String, Integer> tiers, final Predicate<String> hasTier) {
        int best = tiers.getOrDefault("default", 1);
        if (best < 0) return -1;
        for (final Map.Entry<String, Integer> tier : tiers.entrySet()) {
            if (tier.getKey().equals("default") || !hasTier.test(tier.getKey())) continue;
            if (tier.getValue() < 0) return -1;
            best = Math.max(best, tier.getValue());
        }
        return best;
    }

    private int limit(final Player player) {
        return limit(this.limits, tier -> player.hasPermission(this.limitPermPrefix + tier));
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        if (noPermission(sender, cmd)) {
            return true;
        }
        switch (label.toLowerCase(Locale.ROOT)) {
            case "sethome" -> set(sender, args);
            case "delhome" -> delete(sender, args);
            case "homes" -> list(sender, args);
            default -> teleport(sender, args);
        }
        return true;
    }

    /** Who owns the home and its name, from {@code name} or {@code player:name}. Null when refused. */
    private record Target(UUID owner, String ownerName, String home, boolean self) {}

    private Target target(final CommandSender sender, final String arg) {
        final int colon = arg.indexOf(':');
        if (colon < 0) {
            final Player self = playerOrNull(sender);
            return self == null ? null : new Target(self.getUniqueId(), self.getName(), Home.normalize(arg), true);
        }
        if (!hasPermission(sender, this.othersPerm)) {
            sender.sendMessage(format(NO_PERMISSION));
            return null;
        }
        final OfflinePlayer owner = offline(arg.substring(0, colon));
        if (owner == null) {
            sender.sendMessage(format(UNKNOWN_PLAYER, arg.substring(0, colon)));
            return null;
        }
        return new Target(owner.getUniqueId(), owner.getName(), Home.normalize(arg.substring(colon + 1)), false);
    }

    private void set(final CommandSender sender, final String[] args) {
        final Target target = target(sender, args.length == 0 ? Home.DEFAULT_NAME : args[0]);
        final Player player = target == null ? null : playerOrNull(sender);
        if (player == null) return;

        if (!Home.validName(target.home())) {
            sender.sendMessage(format(BAD_NAME));
            return;
        }
        final Location at = player.getLocation();
        if (this.disabledWorlds.contains(at.getWorld().getName()) && !hasPermission(sender, this.othersPerm)) {
            sender.sendMessage(format(DISABLED_WORLD));
            return;
        }

        final Map<String, Home> owned = this.data.homes(target.owner());
        if (target.self() && !owned.containsKey(target.home())) {
            final int max = limit(player);
            if (max >= 0 && owned.size() >= max) {
                sender.sendMessage(format(AT_LIMIT, max));
                return;
            }
        }

        this.data.set(target.owner(), Home.of(target.home(), at));
        sender.sendMessage(format(SET, target.home()));
    }

    private void delete(final CommandSender sender, final String[] args) {
        if (args.length == 0) {
            sender.sendMessage(format(USAGE_DELHOME));
            return;
        }
        final Target target = target(sender, args[0]);
        if (target == null) return;

        if (!this.data.delete(target.owner(), target.home())) {
            sender.sendMessage(format(NOT_FOUND, target.home()));
            return;
        }
        sender.sendMessage(format(DELETED, target.home()));
    }

    private void teleport(final CommandSender sender, final String[] args) {
        final Player player = playerOrNull(sender);
        if (player == null) return;

        final Optional<Home> found;
        final boolean self;
        if (args.length == 0) {
            found = defaultHome(player.getUniqueId());
            self = true;
            if (found.isEmpty()) {
                // nothing obvious to pick, show what they have instead
                list(sender, args);
                return;
            }
        } else {
            final Target target = target(sender, args[0]);
            if (target == null) return;
            found = this.data.home(target.owner(), target.home());
            self = target.self();
            if (found.isEmpty()) {
                sender.sendMessage(format(NOT_FOUND, target.home()));
                return;
            }
        }

        final Home home = found.get();
        final Location location = home.location();
        if (location == null) {
            sender.sendMessage(format(WORLD_GONE, home.world()));
            return;
        }

        this.teleporter.teleport(player, player, self ? this.action : TeleportAction.INSTANT,
                () -> CompletableFuture.completedFuture(location), home.name());
    }

    /** "home" if set, else the only one they have. */
    private Optional<Home> defaultHome(final UUID owner) {
        final Map<String, Home> owned = this.data.homes(owner);
        final Home named = owned.get(Home.DEFAULT_NAME);
        if (named != null) return Optional.of(named);
        return owned.size() == 1 ? owned.values().stream().findFirst() : Optional.empty();
    }

    private void list(final CommandSender sender, final String[] args) {
        final UUID owner;
        final String prefix;
        Component header;
        if (args.length > 0 && !(sender instanceof Player p && p.getName().equalsIgnoreCase(args[0]))) {
            if (!hasPermission(sender, this.othersPerm)) {
                sender.sendMessage(format(NO_PERMISSION));
                return;
            }
            final OfflinePlayer other = offline(args[0]);
            if (other == null) {
                sender.sendMessage(format(UNKNOWN_PLAYER, args[0]));
                return;
            }
            owner = other.getUniqueId();
            prefix = other.getName() + ":";
            header = format(LIST_HEADER_OTHER, other.getName(), this.data.homes(owner).size());
        } else {
            final Player player = playerOrNull(sender);
            if (player == null) return;
            owner = player.getUniqueId();
            prefix = "";
            final int max = limit(player);
            header = format(LIST_HEADER, this.data.homes(owner).size(), max < 0 ? "∞" : max);
        }

        final List<String> names = new ArrayList<>(this.data.homes(owner).keySet());
        if (names.isEmpty() && prefix.isEmpty()) {
            sender.sendMessage(format(NO_HOMES));
            return;
        }
        names.sort(null);

        Component line = Component.empty();
        for (final String name : names) {
            line = line.append(format(LIST_ENTRY, prefix + name));
        }
        sender.sendMessage(header);
        sender.sendMessage(line);
    }

    private static OfflinePlayer offline(final String name) {
        final Player online = Bukkit.getPlayerExact(name);
        return online != null ? online : Bukkit.getOfflinePlayerIfCached(name);
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        if (args.length != 1 || !(sender instanceof Player player)) {
            return List.of();
        }
        if (label.equalsIgnoreCase("homes")) {
            return super.onTabComplete(sender, cmd, label, args);
        }
        final String prefix = args[0].toLowerCase(Locale.ROOT);
        final List<String> names = new ArrayList<>();
        for (final String name : this.data.homes(player.getUniqueId()).keySet()) {
            if (name.startsWith(prefix)) names.add(name);
        }
        names.sort(null);
        return names;
    }
}
