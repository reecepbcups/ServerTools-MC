package sh.reece.core.teleport;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import sh.reece.tools.Main;
import sh.reece.tools.Unloadable;

/**
 * Homes, /back and /rtp, plus the warmup/cooldown/cost rules /tpa shares with them.
 * Each command toggles on its own in config.yml.
 */
public final class TeleportFeature implements Unloadable {

    private final Teleporter teleporter;
    private final HomeCommand homes;

    public TeleportFeature(final Main plugin) {
        this.teleporter = new Teleporter(plugin);
        Bukkit.getPluginManager().registerEvents(this.teleporter, plugin);
        Teleporter.set(this.teleporter);

        this.homes = new HomeCommand(plugin, this.teleporter);
        new BackCommand(plugin, this.teleporter);
        new RandomTeleportCommand(plugin, this.teleporter);
    }

    @Override
    public void onUnload() {
        this.teleporter.cancelAll();
        HandlerList.unregisterAll(this.teleporter);
        Teleporter.set(null);
        this.homes.shutdown();
    }
}
