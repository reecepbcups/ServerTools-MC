package sh.reece.core.hologram;

import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.tools.Unloadable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lifecycle for the holograms in {@code Holograms.yml}. Successor to {@code sh.reece.core.Holograms}; parsing and
 * entities belong to {@link Hologram}, commands to {@link HologramCommand}.
 */
public final class HologramFeature implements Unloadable {

    public static final String SECTION = "Misc.Holograms";
    public static final String CONFIG_FILE = "Holograms.yml";

    private final Main plugin;
    private final ConfigUtils configUtils;
    private final List<Hologram> holograms = new ArrayList<>();
    private final boolean enabled;

    public HologramFeature(final Main plugin) {
        this.plugin = plugin;
        this.configUtils = plugin.getConfigUtils();

        // BaseCommand does the Enabled check and command registration; enabledInConfig also
        // lists the module in /tools, so it must not run a second time here
        this.enabled = new HologramCommand(plugin, this).isEnabled();
        if (!this.enabled) {
            return;
        }

        this.configUtils.createConfig(CONFIG_FILE);
        load();
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public List<Hologram> holograms() {
        return this.holograms;
    }

    public Optional<Hologram> hologram(final String key) {
        return this.holograms.stream()
                .filter((hologram) -> hologram.key().equals(key))
                .findFirst();
    }

    /**
     * Takes down whatever is up, re-reads {@code Holograms.yml}, and draws the result.
     */
    public void load() {
        unload();

        for (final Hologram hologram : Hologram.parseConfig(this.configUtils.getConfigFile(CONFIG_FILE))) {
            // spawn sweeps first, so a cold start clears last run's displays rather than doubling
            if (!hologram.spawn(this.plugin)) {
                Hologram.warn(hologram.key(), "references a world that is not loaded, it will not be drawn");
            }
            this.holograms.add(hologram);
        }
    }

    /**
     * Best-effort on shutdown: Folia halts the region scheduler before disabling plugins, so these tasks may never run.
     * Harmless, because {@code spawn} sweeps before it draws.
     */
    public void unload() {
        this.holograms.forEach((hologram) -> hologram.despawn(this.plugin));
        this.holograms.clear();
    }

    @Override
    public void onUnload() {
        unload();
    }
}
