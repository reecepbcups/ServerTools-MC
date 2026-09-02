package sh.reece.tools;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.bukkit.Bukkit;

import sh.reece.utiltools.MinecraftVersion;
import sh.reece.utiltools.Util;

public class Loader {

	private final Main plugin;
	private final Timings executionTimer;
	private final List<Unloadable> unloadables = new ArrayList<>();
	private Map<String, List<Class<?>>> classIndex;

	public Loader(Main instance) {
		plugin = instance;

		if (MinecraftVersion.getVersion() != MinecraftVersion.SUPPORTED) {
			Util.log("\n\n\n[ServerTools] &cYou are running an unsupported version of Minecraft for this version of the plugin.");
			Util.log("[ServerTools] &eYou can either update to 1.18.x &fOR &euse this version:");
			Util.log("[ServerTools] &ehttps://www.spigotmc.org/resources/servertools-%E2%9E%9C-modular-server-management-1-8-1-18-2-open-source.95853/download?version=455997");
			Util.coloredBroadcast("[ServerTools] This version only supports 1.18+!");
			Util.coloredBroadcast("[ServerTools] Support older versions (<=1.17) with https://www.spigotmc.org/resources/servertools-%E2%9E%9C-modular-server-management-1-8-1-18-2-open-source.95853/download?version=455997!");
			plugin.getServer().getPluginManager().disablePlugin(plugin);
			executionTimer = null;
			return;
		}

		executionTimer = new Timings();
		executionTimer.start();
	}

	private static final String[][] PHASES = {
		{"Commands",      "sh.reece.cmds", "sh.reece.bungee"},
		{"Core Features", "sh.reece.core", "sh.reece.core.warp", "sh.reece.core.economy"},
		{"Chat",          "sh.reece.chat"},
		{"Events",        "sh.reece.events"},
		{"Hoppers",       "sh.reece.hoppers"},
		{"Cooldowns",     "sh.reece.cooldowns"},
		{"Toggleable",    "sh.reece.disabled"},
		{"Moderation",    "sh.reece.moderation"},
		{"GUIs",          "sh.reece.GUI"},
		{"Runnables",     "sh.reece.runnables"},
	};

	public void loadAll() {
		loadPlaceholderAPI();

		// collect only the packages we need
		Set<String> wanted = new java.util.HashSet<>();
		for (String[] phase : PHASES) {
			for (int i = 1; i < phase.length; i++) {
				wanted.add(phase[i]);
			}
		}

		// scan JAR once, only index wanted packages
		classIndex = scanJar(wanted);
		executionTimer.info("JAR Scan");

		for (String[] phase : PHASES) {
			String label = phase[0];
			String[] packages = Arrays.copyOfRange(phase, 1, phase.length);
			discover(label, packages);
		}

		classIndex = null; // free memory
	}

	private void loadPlaceholderAPI() {
		plugin.setPAPIStatus(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"));
		if (Main.isPAPIEnabled()) {
			(new ServerToolsPlaceholders()).register();
			executionTimer.info("PAPI");
		}
	}

	// auto-discovery

	private void discover(String timingLabel, String... packageNames) {
		for (String pkg : packageNames) {
			List<Class<?>> classes = classIndex.getOrDefault(pkg, List.of());
			for (Class<?> clazz : classes) {
				instantiate(clazz);
			}
		}
		executionTimer.info(timingLabel);
	}

	private void instantiate(Class<?> clazz) {
		if (Modifier.isAbstract(clazz.getModifiers())) return;
		if (clazz.getSimpleName().startsWith("_")) return;

		Constructor<?> ctor;
		try {
			ctor = clazz.getConstructor(Main.class);
		} catch (NoSuchMethodException e) {
			return; // not a feature class
		}

		RequiresPlugin req = clazz.getAnnotation(RequiresPlugin.class);
		if (req != null) {
			boolean satisfied = Arrays.stream(req.value())
				.allMatch(dep -> Bukkit.getPluginManager().isPluginEnabled(dep));
			if (!satisfied) {
				Util.consoleMSG("&e" + clazz.getSimpleName() + " skipped (missing: "
					+ String.join(", ", req.value()) + ")");
				return;
			}
		}

		try {
			Object instance = ctor.newInstance(plugin);
			if (instance instanceof Unloadable) {
				unloadables.add((Unloadable) instance);
			}
		} catch (Exception e) {
			Util.log("&cFailed to load " + clazz.getSimpleName() + ": " + e.getMessage());
			if (e.getCause() != null) {
				e.getCause().printStackTrace();
			}
		}
	}

	private Map<String, List<Class<?>>> scanJar(Set<String> wantedPackages) {
		// pre-build path prefixes for fast filtering
		Set<String> wantedPaths = new java.util.HashSet<>();
		for (String pkg : wantedPackages) {
			wantedPaths.add(pkg.replace('.', '/') + "/");
		}

		// pass 1: enumerate entries (cheap - just the central directory) and collect the
		// class names we want, in jar order so instantiation order stays deterministic.
		List<String[]> targets = new ArrayList<>(); // [packageName, className]
		try {
			URI jarUri = plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
			try (JarFile jar = new JarFile(new File(jarUri))) {
				Enumeration<JarEntry> entries = jar.entries();
				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					String name = entry.getName();
					if (!name.endsWith(".class") || name.contains("$")) continue;

					int lastSlash = name.lastIndexOf('/');
					if (lastSlash < 0) continue;

					String dirPath = name.substring(0, lastSlash + 1);
					if (!wantedPaths.contains(dirPath)) continue;

					String packageName = name.substring(0, lastSlash).replace('/', '.');
					String className = name.replace('/', '.').replace(".class", "");
					targets.add(new String[]{packageName, className});
				}
			}
		} catch (Exception e) {
			Util.log("&cFailed to scan JAR: " + e.getMessage());
			return new HashMap<>();
		}

		// pass 2: load classes in parallel. This is the expensive part (bytecode
		// verification + linking); Paper's plugin classloader is parallel-capable, so
		// forName(..., initialize=false) fans out safely across cores with no <clinit>
		// side effects. Correct even if the loader ever serializes - just slower.
		ClassLoader cl = plugin.getClass().getClassLoader();
		Map<String, Class<?>> loaded = new java.util.concurrent.ConcurrentHashMap<>();
		targets.parallelStream().forEach(t -> {
			try {
				loaded.put(t[1], Class.forName(t[1], false, cl));
			} catch (ClassNotFoundException | NoClassDefFoundError ignored) {}
		});

		// pass 3: rebuild the per-package index in the original jar order
		Map<String, List<Class<?>>> index = new HashMap<>();
		for (String[] t : targets) {
			Class<?> clazz = loaded.get(t[1]);
			if (clazz != null) {
				index.computeIfAbsent(t[0], k -> new ArrayList<>()).add(clazz);
			}
		}
		return index;
	}

	// lifecycle

	public void output() {
		String ver = plugin.getDescription().getVersion();
		Util.consoleMSG("\n&b&l[!] ServerTools&b by reecepbcups. Version: " + ver);
		if (plugin.getConfig().getBoolean("LoadWithTimings")) {
			Util.log(executionTimer.end());
		}
	}

	public void setMarking(String mark) {
		if (executionTimer != null) {
			executionTimer.info(mark);
		}
	}

	public void unloadAll() {
		plugin.saveDefaultConfig();
		plugin.getConfigUtils().modulesList.clear();
		// Folia has no global cancelTasks(plugin); cancel the global + async schedulers
		// (this also works on Paper). Per-entity/region tasks are cleaned up when the
		// plugin disables, and each Unloadable cancels its own tasks below.
		sh.reece.utiltools.Schedulers.cancelAll(plugin);
		for (Unloadable u : unloadables) {
			try {
				u.onUnload();
			} catch (Exception e) {
				Util.log("&cError unloading " + u.getClass().getSimpleName() + ": " + e.getMessage());
			}
		}
	}
}
