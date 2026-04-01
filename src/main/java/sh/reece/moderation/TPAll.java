package sh.reece.moderation;

import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;


public class TPAll implements CommandExecutor {//,  {

	private static Main plugin;
	private Random rand;
	private ConfigUtils ConfigUtils;

	public TPAll(Main instance) {
		plugin = instance;
		
		if (plugin.getConfigUtils().enabledInConfig("Moderation.TPAll.Enabled")) {
			ConfigUtils = plugin.getConfigUtils();

			plugin.getCommand("tpall").setExecutor(this);
			rand = new Random();
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		if (!(sender instanceof Player)) {
			sender.sendMessage("This command can only be used by players.");
			return true;
		}
		if (!(sender.hasPermission("tools.tpall"))) {
			sender.sendMessage(Util.color("&cNo Permission to use tpall :("));
			return true;
		}

		Player player = (Player) sender;
		Location loc = player.getLocation();
		
		Util.coloredBroadcast(ConfigUtils.lang("TP_ALL").replace("%player%", player.getName()));
		
		for(Player target : Bukkit.getOnlinePlayers()) {
			new BukkitRunnable(){
				public void run() {	
					if(target != player) {
						target.teleport(loc);
						target.sendMessage(ConfigUtils.lang("TP_ALL_SUCCESS").replace("%player%", player.getName()));
					}										
				}
			}.runTaskLater(plugin, rand.nextInt(100)); // 5 second delay
		}
		
		// 5 second delay to tell them it worked
		new BukkitRunnable(){
			public void run() {	
				player.sendMessage(Util.color("\n&a&o (( All Players Teleported! ))"));									
			}
		}.runTaskLater(plugin, 100); // 5 second delay
		

		return true;
	}


}
