package sh.reece.cmds;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.tools.Unloadable;
import sh.reece.utiltools.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;

public class ChangeSlots extends BaseCommand implements Listener, Unloadable {

	private static String announce;

	private static Main pluginRef;
	public ChangeSlots(Main instance) {
		super(instance, "Commands.ChangeSlots", "changeslots");
		pluginRef = instance;

		if (isEnabled()) {
			announce = instance.getConfig().getString(section + ".AnnounceFullToPermissionedUsers");
		}
	}

	@EventHandler(ignoreCancelled = true)
	public static void onJoin(PlayerJoinEvent e) {
		if (announce.equalsIgnoreCase("true")) {
			if (Bukkit.getServer().getOnlinePlayers().size() == Bukkit.getServer().getMaxPlayers()) {
				Bukkit.broadcast(" ", pluginRef.getConfig().getString("Commands.ChangeSlots.Permission"));
				Bukkit.broadcast(Util.color("&cServer is full! &7&o&n(( " + Bukkit.getServer().getMaxPlayers() + " ))"), pluginRef.getConfig().getString("Commands.ChangeSlots.Permission"));
				Bukkit.broadcast(Util.color("&cBe sure to /changeslots if you want to allow more on!"), pluginRef.getConfig().getString("Commands.ChangeSlots.Permission"));
				Bukkit.broadcast(Util.color("&7&o(( only users with permission see this message ))"), pluginRef.getConfig().getString("Commands.ChangeSlots.Permission"));
				Bukkit.broadcast(" ", pluginRef.getConfig().getString("Commands.ChangeSlots.Permission"));
			}
		}


	}


	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (noPermission(sender, cmd)) {
			return true;
		}
		if (args.length == 0) {
			sender.sendMessage(Util.color("&cPlease put a number. " + "&7&o((Current Max: " + Bukkit.getServer().getMaxPlayers() + "))"));
			return true;
		}
		try {
			changeSlots(Integer.parseInt(args[0]));
			sender.sendMessage(Util.color("&aMax players is now to &e" + args[0]));
		} catch (NumberFormatException e) {
			sender.sendMessage(Util.color("&cPlease put a valid number."));
		} catch (ReflectiveOperationException e) {
			sender.sendMessage(Util.color("&cError! check console"));
			e.printStackTrace();
		}
		return true;


	}

	// CREDIT: https://github.com/MrMicky-FR/ChangeSlots/tree/master/bukkit
	// for below
	public static void saveNewChangeSlotsPlayers() {
		updateServerProperties();
	}

	@Override
	public void onUnload() {
		saveNewChangeSlotsPlayers();
	}

	private void changeSlots(int slots) throws ReflectiveOperationException {
		Method serverGetHandle = plugin.getServer().getClass().getDeclaredMethod("getHandle");
		Object playerList = serverGetHandle.invoke(plugin.getServer());
		Field maxPlayersField = findIntField(playerList, "maxPlayers");
		maxPlayersField.setAccessible(true);
		maxPlayersField.set(playerList, slots);
	}

	/**
	 * Finds the maxPlayers int field by name across the class hierarchy.
	 * Falls back to matching by current value if the field was renamed (e.g. Mojang mappings in 1.20.4+).
	 */
	private Field findIntField(Object playerList, String preferredName) throws ReflectiveOperationException {
		// Try the preferred field name on each superclass first
		for (Class<?> clazz = playerList.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
			try {
				Field f = clazz.getDeclaredField(preferredName);
				if (f.getType() == int.class) return f;
			} catch (NoSuchFieldException ignored) {}
		}

		// Field was renamed/obfuscated - find the int field whose value matches current max players
		int currentMax = Bukkit.getServer().getMaxPlayers();
		for (Class<?> clazz = playerList.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
			for (Field f : clazz.getDeclaredFields()) {
				if (f.getType() == int.class) {
					f.setAccessible(true);
					if (f.getInt(playerList) == currentMax) {
						return f;
					}
				}
			}
		}

		throw new ReflectiveOperationException("Could not find maxPlayers field in " + playerList.getClass().getName());
	}

	private static void updateServerProperties() {

		Properties properties = new Properties();
		File propertiesFile = new File("server.properties");
		try {
			InputStream is = new FileInputStream(propertiesFile);
			try {
				properties.load(is);
				is.close();
			} catch (Throwable throwable) {
				try {
					is.close();
				} catch (Throwable throwable1) {
					throwable.addSuppressed(throwable1);
				}
				throw throwable;
			}

			String maxPlayers = Integer.toString(pluginRef.getServer().getMaxPlayers());
			String currentMax = properties.getProperty("max-players");
			if (currentMax != null && currentMax.equals(maxPlayers)) {
				return;
			}
			properties.setProperty("max-players", maxPlayers);
			OutputStream os = new FileOutputStream(propertiesFile);
			try {
				properties.store(os, "Minecraft server properties");
				os.close();
			} catch (Throwable throwable) {
				try {
					os.close();
				} catch (Throwable throwable1) {
					throwable.addSuppressed(throwable1);
				}
				throw throwable;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}


}
