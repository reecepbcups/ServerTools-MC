package sh.reece.bungee;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class BungeeServerConnector implements Listener {

	private static Main plugin;
	private String Section;
	private String CMD;
	private Set<String> avaliableServers;
	public static final String BUNGEE_CORD_CHANNEL = "BungeeCord";

	public BungeeServerConnector(Main instance) {
		plugin = instance;

		Section = "Bungee.BungeeServerCMD";
		if(plugin.enabledInConfig(Section+".Enabled")) {

			if (!Bukkit.getMessenger().isOutgoingChannelRegistered(plugin, "BungeeCord")) {
				Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
			}

			CMD = plugin.getConfig().getString(Section+".command");
			avaliableServers = plugin.getConfig().getConfigurationSection(Section+".Aliases").getKeys(false);


			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void playerCommandAliasToServerCommand(PlayerCommandPreprocessEvent e) {
		// goto ogskyblock

		String[] msg = e.getMessage().split(" ");
		String myCMD = msg[0].substring(1);
		Player p = e.getPlayer();

		if(!myCMD.equalsIgnoreCase(CMD)) {
			// if not /goto, return
			return;
		}

		// Util.consoleMSG(myCMD); // debug

		if(!(msg.length >= 2)) { // if there is not an argument (args[0] and [1] = len 2)
			sendHelpMenu(p, myCMD);
			e.setCancelled(true);
			return;
		}

		// if the argument is a config key from the config, connect
		if(avaliableServers.contains(msg[1])) {
			//Util.consoleMSG("Connecting");
			connect(p, plugin.getConfig().getString(Section+".Aliases."+msg[1]));
		} else {
			Util.coloredMessage(p, "\n&cThis server is not avaliable...");
			sendHelpMenu(p, myCMD);
		}
		e.setCancelled(true);


	}

	public void sendHelpMenu(Player p, String cmd) {
		Util.coloredMessage(p, "&f/"+cmd+" &7<server>");
		Util.coloredMessage(p, avaliableServers.toString());
	}

	public static void connect(Player player, String server) {
		if (server.length() == 0) {
			Util.coloredMessage(player, "&cTarget server was an empty string, cannot connect to it.");
			return;
		}
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
		try {
			dataOutputStream.writeUTF("Connect");
			dataOutputStream.writeUTF(server);
		} catch (IOException ex) {
			throw new AssertionError();
		}
		player.sendPluginMessage(plugin, "BungeeCord", byteArrayOutputStream.toByteArray());
	}

}
