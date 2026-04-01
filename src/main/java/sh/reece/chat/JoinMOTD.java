package sh.reece.chat;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;

import me.clip.placeholderapi.PlaceholderAPI;
import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;
import sh.reece.utiltools.Util;

public class JoinMOTD extends ToggleableListener {

	private List<String> MOTDMsg;
	private Boolean papiSupport;

	public JoinMOTD(Main instance) {
		super(instance, "Events.ChatJoinMOTD");

		if (isEnabled()) {
			String Section = "Events.ChatJoinMOTD";
			MOTDMsg = instance.getConfig().getStringList(Section + ".MOTD");

			if (MOTDMsg != null && MOTDMsg.size() > 0) {
				papiSupport = Main.isPAPIEnabled();
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent e) {
		Player p = e.getPlayer();

		for (String msgLine : MOTDMsg) {

			msgLine = ConfigUtils.replaceVariable(msgLine);

			if (papiSupport) {
				msgLine = PlaceholderAPI.setPlaceholders(p, msgLine);
			}

			if (msgLine.contains("<center>")) {
				msgLine = Util.centerMessage(msgLine.replace("<center>", "")
						.replace("%player%", p.getName()));
			}

			Util.coloredMessage(p, msgLine);
		}
	}
}
