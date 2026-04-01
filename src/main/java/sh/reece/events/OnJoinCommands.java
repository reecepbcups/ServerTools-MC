package sh.reece.events;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class OnJoinCommands implements Listener {


	private static Main plugin;
	private List<String> FirstJoinCMDS, PlayerRunOnJoin;
	private Boolean isFirstJoinEnabled, isPlayerRunEnabled;

	public OnJoinCommands(Main instance) {
		plugin = instance;

		String section = "Misc.OnJoinCommands";
		if (plugin.enabledInConfig(section+".Enabled")) {

			//FileConfiguration config = plugin.getConfig();
			FirstJoinCMDS = plugin.getConfig().getStringList(section+".FirstUniqueJoin.CMDS");
			PlayerRunOnJoin = plugin.getConfig().getStringList(section+".PlayerRunCommands.CMDS");

			isFirstJoinEnabled = plugin.getConfig().getString(section+".FirstUniqueJoin.Enabled").equalsIgnoreCase("true");
			isPlayerRunEnabled = plugin.getConfig().getString(section+".PlayerRunCommands.Enabled").equalsIgnoreCase("true");

			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
		}
	}



	@EventHandler(ignoreCancelled = true)
	public void PlayerCommand(PlayerJoinEvent event) {
		Player p = (Player) event.getPlayer();

		// First Join commands enabled & Player has not played before
		if(isFirstJoinEnabled) {
			if(!(p.hasPlayedBefore())){
				new BukkitRunnable() {
					@Override
					public void run() {
						for (String cmd : FirstJoinCMDS) { Util.console(cmd.replace("%player%", p.getName())); }
					}
				}.runTaskLater(plugin, 10L);

			}
		}

		if(isPlayerRunEnabled) {
			new BukkitRunnable() {
				@Override
				public void run() {
					for (String command : PlayerRunOnJoin) { p.performCommand(command.replace("%player%", p.getName())); }
				}
			}.runTaskLater(plugin, 10L);
		}
	}

}
