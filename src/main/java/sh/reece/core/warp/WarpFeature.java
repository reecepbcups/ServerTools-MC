package sh.reece.core.warp;

import org.bukkit.configuration.file.FileConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.tools.Unloadable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class WarpFeature implements Unloadable {

    private static final Logger LOGGER = LoggerFactory.getLogger(WarpFeature.class);

    private final Main plugin;
    private final ConfigUtils configUtils;
    private final boolean enabled;

    private final Map<String, Warp> warps = new LinkedHashMap<>();
    private WarpConfiguration configuration;

    public WarpFeature(final Main plugin) {
        this.plugin = plugin;
        this.configUtils = plugin.getConfigUtils();

        // BaseCommand does the Enabled check and command registration; enabledInConfig also
        // lists the module in /tools, so it must not run a second time here
        this.enabled = new WarpComand(plugin, this).isEnabled();
        if (!this.enabled) {
            return;
        }

        this.configUtils.createConfig(Warp.WARPS_FILE);
        load();
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public WarpConfiguration configuration() {
        return this.configuration;
    }

    public Collection<Warp> warps() {
        return this.warps.values();
    }

    public Optional<Warp> warp(final String name) {
        return Optional.ofNullable(this.warps.get(name));
    }

    /**
     * Drops what is held and re-reads {@code Warps.yml} and the permission nodes. Entries that fail to parse
     * are skipped and logged by {@link Warp#parseWarp}, so a bad one costs only itself.
     */
    public void load() {
        this.configuration = WarpConfiguration.fromConfig(this.plugin.getConfig());

        final FileConfiguration file = this.configUtils.getConfigFile(Warp.WARPS_FILE);
        this.warps.clear();
        for (final Warp warp : Warp.parseWarps(file)) {
            this.warps.put(warp.name(), warp);
        }

        LOGGER.info("Loaded {} warps", this.warps.size());
    }

    @Override
    public void onUnload() {
        this.warps.clear();
    }
}
