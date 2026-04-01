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
import sh.reece.tools.ToggleableListener;
import sh.reece.utiltools.Util;

public class BungeeServerConnector extends ToggleableListener {

	private static Main pluginRef;
	private String Section;
	private String CMD;
	private Set<String> avaliableServers;
	public static final String BUNGEE_CORD_CHANNEL = "BungeeCord";

	public BungeeServerConnector(Main instance) {
		super(instance, "Bungee.BungeeServerCMD");
		pluginRef = instance;

		if(isEnabled()) {
			Section = "Bungee.BungeeServerCMD";

			if (!Bukkit.getMessenger().isOutgoingChannelRegistered(instance, "BungeeCord")) {
				Bukkit.getMessenger().registerOutgoingPluginChannel(instance, "BungeeCord");
			}

			CMD = instance.getConfig().getString(Section+".command");
			var aliasSection = instance.getConfig().getConfigurationSection(Section+".Aliases");
			if (aliasSection == null) {
				Util.consoleMSG("&c[BungeeServerConnector] Missing Aliases section in config");
				avaliableServers = java.util.Collections.emptySet();
			} else {
				avaliableServers = aliasSection.getKeys(false);
			}
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
			connect(p, pluginRef.getConfig().getString("Bungee.BungeeServerCMD.Aliases."+msg[1]));
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
		player.sendPluginMessage(pluginRef, "BungeeCord", byteArrayOutputStream.toByteArray());
	}

}
