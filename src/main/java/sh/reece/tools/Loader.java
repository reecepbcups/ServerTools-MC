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

	public void loadAll() {
		loadPlaceholderAPI();

		// scan JAR once, bucket classes by package
		classIndex = scanJar();

		discover("Commands",      "sh.reece.cmds", "sh.reece.bungee");
		discover("Core Features", "sh.reece.core", "sh.reece.core.warp");
		discover("Chat",          "sh.reece.chat");
		discover("Events",        "sh.reece.events");
		discover("Cooldowns",     "sh.reece.cooldowns");
		discover("Toggleable",    "sh.reece.disabled");
		discover("Moderation",    "sh.reece.moderation");
		discover("GUIs",          "sh.reece.GUI");
		discover("Runnables",     "sh.reece.runnables");

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

	private Map<String, List<Class<?>>> scanJar() {
		Map<String, List<Class<?>>> index = new HashMap<>();
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

					String packageName = name.substring(0, lastSlash).replace('/', '.');
					String className = name.replace('/', '.').replace(".class", "");
					try {
						Class<?> clazz = Class.forName(className, false, plugin.getClass().getClassLoader());
						index.computeIfAbsent(packageName, k -> new ArrayList<>()).add(clazz);
					} catch (ClassNotFoundException | NoClassDefFoundError ignored) {}
				}
			}
		} catch (Exception e) {
			Util.log("&cFailed to scan JAR: " + e.getMessage());
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
		executionTimer.info(mark);
	}

	public void unloadAll() {
		plugin.saveDefaultConfig();
		plugin.getConfigUtils().modulesList.clear();
		Bukkit.getServer().getScheduler().cancelTasks(plugin);
		for (Unloadable u : unloadables) {
			try {
				u.onUnload();
			} catch (Exception e) {
				Util.log("&cError unloading " + u.getClass().getSimpleName() + ": " + e.getMessage());
			}
		}
	}
}
