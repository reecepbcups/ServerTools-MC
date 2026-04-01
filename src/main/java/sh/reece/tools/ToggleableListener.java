package sh.reece.tools;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

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
                ? plugin.getConfig().getString(permPath) : null;
            Bukkit.getPluginManager().registerEvents(this, plugin);
        } else {
            this.permission = null;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}
