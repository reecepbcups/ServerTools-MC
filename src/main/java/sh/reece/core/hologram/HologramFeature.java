package sh.reece.core.hologram;

import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.tools.Unloadable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lifecycle for the holograms in {@code Holograms.yml}; parsing and entities belong to {@link Hologram}, commands
 * to {@link HologramCommand}.
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
        final List<Hologram> parsed = Hologram.parseConfig(this.configUtils.getConfigFile(CONFIG_FILE));
        final Set<String> keys = parsed.stream().map(Hologram::key).collect(Collectors.toSet());

        // a key that left the config since the last load keeps its displays otherwise:
        // spawn() only sweeps around the holograms it is about to draw. Keys that stayed
        // are left alone so their despawn cannot race the redraw below.
        this.holograms.stream()
                .filter((hologram) -> !keys.contains(hologram.key()))
                .forEach((hologram) -> hologram.despawn(this.plugin));
        this.holograms.clear();

        // one-time migration off the pre-rewrite class: its displays carried no PDC tag, so the
        // normal sweep skips them and lines would render twice after the switchover. Sweep them
        // at each configured location on the first load after the version bump only. The version
        // is written by loadConfig at startup, so this is false again on the next restart.
        final boolean sweepLegacy = this.configUtils.isVersionChanged();

        for (final Hologram hologram : parsed) {
            // spawn sweeps first, so a cold start clears last run's displays rather than doubling
            if (!hologram.spawn(this.plugin, sweepLegacy)) {
                Hologram.warn(hologram.key(), "references a world that is not loaded, it will not be drawn");
            }
            this.holograms.add(hologram);
        }
    }

    @Override
    public void onUnload() {
        this.holograms.forEach((hologram) -> hologram.despawn(this.plugin));
        this.holograms.clear();
    }
}
