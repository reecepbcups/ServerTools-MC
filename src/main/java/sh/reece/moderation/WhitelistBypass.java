package sh.reece.moderation;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class WhitelistBypass implements Listener {

	private static Main plugin;
	public String Whitelistperm, Message;
	private Boolean DisableWhitelistCMDInGame;
	
	public WhitelistBypass(Main instance) {
		plugin = instance;

		if (plugin.getConfigUtils().enabledInConfig("Moderation.WhitelistBypass.Enabled")) {
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);	

			this.Whitelistperm = plugin.getConfig().getString("Moderation.WhitelistBypass.Permission", "whitelist.bypass");
			this.Message = Util.color(plugin.getConfig().getString("Moderation.WhitelistBypass.Message"));
			
			DisableWhitelistCMDInGame = plugin.getConfig().getBoolean("Moderation.WhitelistBypass.DisableWhitelistCMDInGame");
			
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onLogin(PlayerLoginEvent e) {
		if (e.getResult() == PlayerLoginEvent.Result.KICK_WHITELIST) {

			if(e.getPlayer().hasPermission(Whitelistperm) || e.getPlayer().isWhitelisted()) {
				e.setResult(PlayerLoginEvent.Result.ALLOWED);
				return;
			}

			// if whitelist is on AND player is not whitelisted
			if (Bukkit.hasWhitelist() && !e.getPlayer().isWhitelisted()) {				
				Message = Message.replaceAll("%player%", e.getPlayer().getName());
				e.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, Message);
			} 

		}
	}

	
	@EventHandler(ignoreCancelled = true) // does not affect console, for that ServerCommandEvent would be needed
	public void playerCMD(PlayerCommandPreprocessEvent e) {
		if(DisableWhitelistCMDInGame && e.getMessage().startsWith("/whitelist add")) {
			Util.coloredMessage(e.getPlayer(), "&e[ServerTools] &cRunning /whitelist add is disabled! &fPlease perform this in console if you need to use, or give user permission: &e" + Whitelistperm);
			e.setCancelled(true);
		}
		
	}





}
