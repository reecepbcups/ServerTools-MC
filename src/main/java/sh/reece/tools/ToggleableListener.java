package sh.reece.tools;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permissible;

public abstract class ToggleableListener implements Listener {

    protected final Main plugin;
    protected final String permission;
    private final boolean enabled;
    // Optional world scoping. Empty = applies in every world (backwards compatible).
    private final List<String> worlds;

    protected ToggleableListener(Main plugin, String section) {
        this.plugin = plugin;
        this.enabled = plugin.getConfigUtils().enabledInConfig(section + ".Enabled");
        if (enabled) {
            String permPath = section + ".Permission";
            String legacyPath = section + ".BypassPerm";
            if (plugin.getConfig().contains(permPath)) {
                this.permission = plugin.getConfig().getString(permPath, "");
            } else if (plugin.getConfig().contains(legacyPath)) {
                this.permission = plugin.getConfig().getString(legacyPath, "");
            } else {
                this.permission = "";
            }
            this.worlds = plugin.getConfig().getStringList(section + ".Worlds");
            Bukkit.getPluginManager().registerEvents(this, plugin);
        } else {
            this.permission = "";
            this.worlds = List.of();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    // True if this listener should act in the given world. An empty WorldsToDisable list
    // means "every world" so existing features that don't set it keep working unchanged.
    protected boolean appliesInWorld(World world) {
        return worlds.isEmpty() || worlds.contains(world.getName());
    }

    protected boolean hasPermission(Permissible who) {
        return !permission.isEmpty() && (who.isOp() || who.hasPermission(permission));
    }
}
