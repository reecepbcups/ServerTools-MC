package sh.reece.utiltools;

import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

/**
 * Thin wrapper over Paper/Folia's region schedulers.
 *
 * These schedulers (global/region/entity/async) all exist on modern Paper (1.20+)
 * too, so the same call works on both platforms - no forked build and no reflection,
 * except the one isFolia() check used to gate the global hopper engine that can't
 * exist under Folia's region threading.
 *
 * Delays/periods are in ticks (20/sec) except the async* helpers, which take millis.
 * Folia rejects a delay/period of 0, so repeating timers clamp to a minimum of 1 tick.
 */
public final class Schedulers {

	private Schedulers() {}

	private static final boolean FOLIA = detectFolia();

	private static boolean detectFolia() {
		try {
			Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	/** True when running on Folia (region-threaded), false on Paper/Spigot. */
	public static boolean isFolia() {
		return FOLIA;
	}

	// --- global: broadcasts, console commands, countdowns, metrics ---

	public static ScheduledTask global(Plugin plugin, Runnable task) {
		return Bukkit.getGlobalRegionScheduler().run(plugin, t -> task.run());
	}

	public static ScheduledTask globalLater(Plugin plugin, Runnable task, long delayTicks) {
		if (delayTicks <= 0) return global(plugin, task);
		return Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> task.run(), delayTicks);
	}

	public static ScheduledTask globalTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
		return Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> task.run(),
			Math.max(1, delayTicks), Math.max(1, periodTicks));
	}

	// --- entity: per-player mutations and teleport follow-ups ---
	// the task is skipped if the entity has been removed by the time it would run.

	public static ScheduledTask entity(Plugin plugin, Entity entity, Runnable task) {
		return entity.getScheduler().run(plugin, t -> task.run(), null);
	}

	public static ScheduledTask entityLater(Plugin plugin, Entity entity, Runnable task, long delayTicks) {
		return entity.getScheduler().runDelayed(plugin, t -> task.run(), null, Math.max(1, delayTicks));
	}

	public static ScheduledTask entityTimer(Plugin plugin, Entity entity, Runnable task, long delayTicks, long periodTicks) {
		return entity.getScheduler().runAtFixedRate(plugin, t -> task.run(), null,
			Math.max(1, delayTicks), Math.max(1, periodTicks));
	}

	// --- region: block/world access anchored to a location or chunk ---

	public static ScheduledTask region(Plugin plugin, Location loc, Runnable task) {
		return Bukkit.getRegionScheduler().run(plugin, loc, t -> task.run());
	}

	public static ScheduledTask regionLater(Plugin plugin, Location loc, Runnable task, long delayTicks) {
		return Bukkit.getRegionScheduler().runDelayed(plugin, loc, t -> task.run(), Math.max(1, delayTicks));
	}

	public static void regionChunk(Plugin plugin, World world, int chunkX, int chunkZ, Runnable task) {
		Bukkit.getRegionScheduler().execute(plugin, world, chunkX, chunkZ, task);
	}

	// --- async: off-thread I/O and debug work ---

	public static ScheduledTask async(Plugin plugin, Runnable task) {
		return Bukkit.getAsyncScheduler().runNow(plugin, t -> task.run());
	}

	public static ScheduledTask asyncLater(Plugin plugin, Runnable task, long delayMillis) {
		return Bukkit.getAsyncScheduler().runDelayed(plugin, t -> task.run(), Math.max(1, delayMillis), TimeUnit.MILLISECONDS);
	}

	/** Cancels every global + async task owned by the plugin. Called from the unload path. */
	public static void cancelAll(Plugin plugin) {
		Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
		Bukkit.getAsyncScheduler().cancelTasks(plugin);
	}
}
