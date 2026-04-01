package sh.reece.cmds;

import sh.reece.tools.AlternateCommandHandler;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;

public class ChangeSlots implements CommandExecutor, Listener {


	private static String permission, announce;

	private static Main plugin;
	public ChangeSlots(Main instance) {
		plugin = instance;

		String section = "Commands.ChangeSlots";
		if (plugin.enabledInConfig(section+".Enabled")) {
			permission = plugin.getConfig().getString(section+".Permission");
			announce = plugin.getConfig().getString(section+".AnnounceFullToPermissionedUsers");
			plugin.getCommand("changeslots").setExecutor(this);
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
		} else {
			AlternateCommandHandler.addDisableCommand("changeslots");
		}
	}

	@EventHandler(ignoreCancelled = true)
	public static void onJoin(PlayerJoinEvent e) {
		if (announce.equalsIgnoreCase("true")) {
			if (Bukkit.getServer().getOnlinePlayers().size() == Bukkit.getServer().getMaxPlayers()) {
				Bukkit.broadcast(" ", permission);
				Bukkit.broadcast(Util.color("&cServer is full! &7&o&n(( " + Bukkit.getServer().getMaxPlayers() + " ))"), permission);
				Bukkit.broadcast(Util.color("&cBe sure to /changeslots if you want to allow more on!"), permission);
				Bukkit.broadcast(Util.color("&7&o(( only users with " + permission + " see this message ))"), permission);
				Bukkit.broadcast(" ", permission);
			}
		}


	}


	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (!sender.hasPermission(permission)) {
			sender.sendMessage("No permission: " + permission);
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

	private void changeSlots(int slots) throws ReflectiveOperationException {
		Method serverGetHandle = plugin.getServer().getClass().getDeclaredMethod("getHandle");
		Object playerList = serverGetHandle.invoke(plugin.getServer());
		Field maxPlayersField = playerList.getClass().getSuperclass().getDeclaredField("maxPlayers");
		maxPlayersField.setAccessible(true);
		maxPlayersField.set(playerList, slots);
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

			String maxPlayers = Integer.toString(plugin.getServer().getMaxPlayers());
			if (properties.getProperty("max-players").equals(maxPlayers)) {
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
