package sh.reece.chat;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import me.clip.placeholderapi.PlaceholderAPI;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class JoinMOTD implements Listener {

	private static Main plugin;
	private String Section;
	private List<String> MOTDMsg;
	private Boolean papiSupport;

	public JoinMOTD(Main instance) {
        plugin = instance;

        Section = "Events.ChatJoinMOTD";
        if(plugin.enabledInConfig(Section+".Enabled")) {

        	MOTDMsg = plugin.getConfig().getStringList(Section+".MOTD");

			if(MOTDMsg != null && MOTDMsg.size() > 0) {
				Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
				papiSupport = plugin.isPAPIEnabled();
			}
    	}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent e) {
		Player p = e.getPlayer();

		for(String msgLine : MOTDMsg) {

			msgLine = Main.replaceVariable(msgLine);

			if(papiSupport) {
				msgLine = PlaceholderAPI.setPlaceholders(p, msgLine);
			}

			if(msgLine.contains("<center>")) {
				msgLine = Util.centerMessage(msgLine.replace("<center>", "")
						.replace("%player%", p.getName()));
			}

			Util.coloredMessage(p, msgLine);
		}

	}

}
