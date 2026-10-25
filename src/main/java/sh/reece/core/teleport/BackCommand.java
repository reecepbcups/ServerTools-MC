package sh.reece.core.teleport;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static sh.reece.utiltools.TextUtil.format;

/**
 * /back to the spot before the last command teleport, or the death spot for players with the death perm.
 * Kept in memory only, so a restart clears it.
 */
final class BackCommand extends BaseCommand implements Listener {

    static final String SECTION = "Core.Back";

    // portals, pearls and chorus fruit aren't worth going back from
    private static final Set<TeleportCause> TRACKED = Set.of(TeleportCause.COMMAND, TeleportCause.PLUGIN, TeleportCause.UNKNOWN);

    private static final String NOTHING = "<red>You have nowhere to go back to.";
    private static final String WORLD_GONE = "<red>That world isn't loaded anymore.";

    private final Map<UUID, Location> last = new ConcurrentHashMap<>();
    private final Teleporter teleporter;
    private final TeleportAction action;
    private final String onDeathPerm;
    private final List<String> disabledWorlds;

    BackCommand(final Main plugin, final Teleporter teleporter) {
        super(plugin, SECTION, "back");
        this.teleporter = teleporter;
        final FileConfiguration config = plugin.getConfig();
        this.action = TeleportAction.fromConfig(config, SECTION);
        this.onDeathPerm = config.getString(SECTION + ".OnDeathPerm", "tools.back.ondeath");
        this.disabledWorlds = config.getStringList(SECTION + ".DisabledWorlds");
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        if (noPermission(sender, cmd)) {
            return true;
        }
        final Player player = playerOrNull(sender);
        if (player == null) {
            return true;
        }

        final Location back = this.last.get(player.getUniqueId());
        if (back == null) {
            sender.sendMessage(format(NOTHING));
            return true;
        }
        if (!back.isWorldLoaded()) {
            this.last.remove(player.getUniqueId());
            sender.sendMessage(format(WORLD_GONE));
            return true;
        }

        this.teleporter.teleport(player, player, this.action, () -> CompletableFuture.completedFuture(back), "your last location");
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(final PlayerTeleportEvent event) {
        if (TRACKED.contains(event.getCause())) {
            remember(event.getPlayer(), event.getFrom());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(final PlayerDeathEvent event) {
        final Player player = event.getEntity();
        if (hasPermission(player, this.onDeathPerm)) {
            remember(player, player.getLocation());
        }
    }

    private void remember(final Player player, final Location from) {
        if (!this.disabledWorlds.contains(from.getWorld().getName())) {
            this.last.put(player.getUniqueId(), from.clone());
        }
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        return List.of();
    }
}
