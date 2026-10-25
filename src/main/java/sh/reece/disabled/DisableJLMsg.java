package sh.reece.disabled;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.clip.placeholderapi.PlaceholderAPI;
import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;
import sh.reece.utiltools.Util;

public class DisableJLMsg extends ToggleableListener {

	public static final String SECTION = "Disabled.DisableJoinLeaveMsg";

	// Empty = hide the message (original behaviour).
	private final String joinMsg;
	private final String quitMsg;

	public DisableJLMsg(Main instance) {
		super(instance, SECTION);
		joinMsg = instance.getConfig().getString(SECTION + ".JoinMessage", "");
		quitMsg = instance.getConfig().getString(SECTION + ".QuitMessage", "");
	}


	@EventHandler(ignoreCancelled = true)
	public void onJoin(PlayerJoinEvent e) {
		e.setJoinMessage(format(e.getPlayer(), joinMsg));

		if(e.getPlayer().getUniqueId().toString().equalsIgnoreCase("79da3753-1b9e-4340-8a0f-9ea975c17fe4")) {
			e.getPlayer().sendMessage("This server uses your ServerTools Plugin!");
		}
	}


	@EventHandler(ignoreCancelled = true)
	public void onQuit(PlayerQuitEvent e) {
		e.setQuitMessage(format(e.getPlayer(), quitMsg));
	}

	// public so Vanish can send the same custom messages for fake join/quit.
	public static String format(Player p, String msg) {
		if (msg.isEmpty()) {
			return "";
		}
		msg = ConfigUtils.replaceVariable(msg)
				.replace("%player%", p.getName())
				.replace("%displayname%", p.getDisplayName());
		if (Main.isPAPIEnabled()) {
			msg = PlaceholderAPI.setPlaceholders(p, msg);
		}
		return Util.color(msg);
	}

}
