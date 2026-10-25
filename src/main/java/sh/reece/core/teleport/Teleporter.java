package sh.reece.core.teleport;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import sh.reece.tools.Main;
import sh.reece.utiltools.Schedulers;
import sh.reece.utiltools.TimeUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static sh.reece.utiltools.TextUtil.format;

/**
 * The shared warmup, cooldown and cost rules behind /home, /back, /rtp and /tpa.
 * Cooldowns are memory only, so a restart clears them.
 */
public final class Teleporter implements Listener {

    static final String SECTION = "Core.TeleportSettings";

    private static final String WARMUP = "<gray>Teleporting in <white>%s</white>, don't move.";
    private static final String CANCELLED = "<red>Teleport cancelled.";
    private static final String ON_COOLDOWN = "<red>You can do that again in <white>%s</white>.";
    private static final String NO_MONEY = "<red>You need <white>%s</white> to do that.";
    private static final String CHARGED = "<gray>Charged <white>%s</white>.";
    private static final String NO_DESTINATION = "<red>Couldn't find a place to send you. Try again.";
    private static final String TELEPORTED = "<green>Teleported to <white>%s</white>.";

    private static volatile Teleporter instance;

    private record Warmup(ScheduledTask task, UUID world, int x, int y, int z) {}

    private final Main plugin;
    private final boolean cancelOnMove;
    private final boolean cancelOnDamage;
    private final String bypassWarmupPerm;
    private final String bypassCooldownPerm;
    private final String bypassCostPerm;

    private final Map<UUID, Warmup> warmups = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> readyAt = new ConcurrentHashMap<>();

    Teleporter(final Main plugin) {
        this.plugin = plugin;
        final FileConfiguration config = plugin.getConfig();
        this.cancelOnMove = config.getBoolean(SECTION + ".CancelWarmupOnMove", true);
        this.cancelOnDamage = config.getBoolean(SECTION + ".CancelWarmupOnDamage", true);
        this.bypassWarmupPerm = config.getString(SECTION + ".BypassWarmupPerm", "tools.teleport.bypass.warmup");
        this.bypassCooldownPerm = config.getString(SECTION + ".BypassCooldownPerm", "tools.teleport.bypass.cooldown");
        this.bypassCostPerm = config.getString(SECTION + ".BypassCostPerm", "tools.teleport.bypass.cost");
    }

    /** Null before the teleport feature loads or after it unloads. */
    public static Teleporter get() {
        return instance;
    }

    static void set(final Teleporter teleporter) {
        instance = teleporter;
    }

    /** Checks cooldown and balance without charging. Tells the player why when it says no. */
    public boolean canStart(final Player payer, final TeleportAction action) {
        final long wait = cooldownLeft(payer, action);
        if (wait > 0) {
            payer.sendMessage(format(ON_COOLDOWN, TimeUtil.formatDuration((wait + 999) / 1000)));
            return false;
        }
        if (chargeable(payer, action)) {
            final Economy economy = economy();
            if (economy != null && !economy.has(payer, action.cost())) {
                payer.sendMessage(format(NO_MONEY, economy.format(action.cost())));
                return false;
            }
        }
        return true;
    }

    /**
     * Waits out the warmup, then sends {@code mover} to wherever {@code destination} resolves. The destination is
     * only looked up after the warmup, so a moving target or an rtp search uses fresh data. {@code payer} is charged
     * and put on cooldown only once the teleport actually lands.
     */
    public void teleport(final Player mover, final Player payer, final TeleportAction action,
                         final Supplier<CompletableFuture<Location>> destination, final String label) {
        if (!canStart(payer, action)) {
            return;
        }
        cancel(mover, false);

        final int seconds = mover.hasPermission(this.bypassWarmupPerm) ? 0 : action.warmupSeconds();
        if (seconds <= 0) {
            go(mover, payer, action, destination, label);
            return;
        }

        mover.sendMessage(format(WARMUP, TimeUtil.formatDuration(seconds)));
        final Location at = mover.getLocation();
        final ScheduledTask task = Schedulers.entityLater(this.plugin, mover, () -> {
            this.warmups.remove(mover.getUniqueId());
            go(mover, payer, action, destination, label);
        }, seconds * 20L);
        this.warmups.put(mover.getUniqueId(),
                new Warmup(task, at.getWorld().getUID(), at.getBlockX(), at.getBlockY(), at.getBlockZ()));
    }

    private void go(final Player mover, final Player payer, final TeleportAction action,
                    final Supplier<CompletableFuture<Location>> destination, final String label) {
        destination.get().whenComplete((location, error) -> Schedulers.entity(this.plugin, mover, () -> {
            if (error != null || location == null) {
                mover.sendMessage(format(NO_DESTINATION));
                return;
            }
            // balance could have changed during the warmup
            if (!canStart(payer, action)) {
                return;
            }
            mover.teleportAsync(location).thenAccept(landed -> {
                if (!landed) return;
                charge(payer, action);
                mover.sendMessage(format(TELEPORTED, label));
            });
        }));
    }

    private void charge(final Player payer, final TeleportAction action) {
        if (action.cooldownSeconds() > 0 && !payer.hasPermission(this.bypassCooldownPerm)) {
            this.readyAt.computeIfAbsent(payer.getUniqueId(), k -> new ConcurrentHashMap<>())
                    .put(action.key(), System.currentTimeMillis() + action.cooldownSeconds() * 1000L);
        }
        if (chargeable(payer, action)) {
            final Economy economy = economy();
            if (economy != null) {
                economy.withdrawPlayer(payer, action.cost());
                payer.sendMessage(format(CHARGED, economy.format(action.cost())));
            }
        }
    }

    private long cooldownLeft(final Player payer, final TeleportAction action) {
        if (action.cooldownSeconds() <= 0 || payer.hasPermission(this.bypassCooldownPerm)) {
            return 0;
        }
        final Map<String, Long> times = this.readyAt.get(payer.getUniqueId());
        final Long ready = times == null ? null : times.get(action.key());
        return ready == null ? 0 : ready - System.currentTimeMillis();
    }

    private boolean chargeable(final Player payer, final TeleportAction action) {
        return action.cost() > 0 && !payer.hasPermission(this.bypassCostPerm);
    }

    private void cancel(final Player player, final boolean tell) {
        final Warmup warmup = this.warmups.remove(player.getUniqueId());
        if (warmup == null) return;
        warmup.task().cancel();
        if (tell) {
            player.sendMessage(format(CANCELLED));
        }
    }

    void cancelAll() {
        this.warmups.values().forEach(w -> w.task().cancel());
        this.warmups.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(final PlayerMoveEvent event) {
        // runs for every step on the server, so bail before any lookup when nobody is warming up
        if (!this.cancelOnMove || this.warmups.isEmpty() || !event.hasChangedBlock()) return;

        final Warmup warmup = this.warmups.get(event.getPlayer().getUniqueId());
        if (warmup == null) return;
        final Location to = event.getTo();
        if (!to.getWorld().getUID().equals(warmup.world()) || to.getBlockX() != warmup.x()
                || to.getBlockY() != warmup.y() || to.getBlockZ() != warmup.z()) {
            cancel(event.getPlayer(), true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(final EntityDamageEvent event) {
        if (this.cancelOnDamage && !this.warmups.isEmpty() && event.getEntity() instanceof Player player) {
            cancel(player, true);
        }
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        cancel(event.getPlayer(), false);
    }

    // Vault is a soft dependency, so never touch its classes unless the plugin is there
    private static Economy economy() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            return null;
        }
        final RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
        return provider == null ? null : provider.getProvider();
    }
}
