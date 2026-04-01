package sh.reece.tools;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permissible;

public abstract class ToggleableListener implements Listener {

    protected final Main plugin;
    protected final String permission;
    private final boolean enabled;

    protected ToggleableListener(Main plugin, String section) {
        this.plugin = plugin;
        this.enabled = plugin.getConfigUtils().enabledInConfig(section + ".Enabled");
        if (enabled) {
            String permPath = section + ".Permission";
            this.permission = plugin.getConfig().contains(permPath)
                ? plugin.getConfig().getString(permPath, "") : "";
            Bukkit.getPluginManager().registerEvents(this, plugin);
        } else {
            this.permission = "";
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    protected boolean hasPermission(Permissible who) {
        return !permission.isEmpty() && (who.isOp() || who.hasPermission(permission));
    }
}
