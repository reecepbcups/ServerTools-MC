package sh.reece.core;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import sh.reece.tools.AlternateCommandHandler;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class God implements CommandExecutor, Listener {//,TabCompleter,Listener {

	String Section, Permission;
	private Main plugin;
	public God(Main instance) {
		this.plugin = instance;

		Section = "Core.God";        

		// https://essinfo.xeya.me/permissions.html
		if(plugin.enabledInConfig(Section+".Enabled")) {
			plugin.getCommand("god").setExecutor(this);
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
			
			Permission = plugin.getConfig().getString(Section+".Permission");
		} else {
			AlternateCommandHandler.addDisableCommand("god");
		}
		
	}
	
	private static Set<Player> GODS = new HashSet<>();
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {		
		if (!sender.hasPermission(Permission)) {
			sender.sendMessage(Util.color("&cYou do not have access to &n/" +label+"&c."));
			return true;
		} 
		
		Player p = (Player) sender;
		
		boolean toggled = GODS.contains(p) ? GODS.remove(p) : GODS.add(p);
		Util.coloredMessage(p, "&f[!] &fGod mode " + (GODS.contains(p) ? "&aenabled" : "&cdisabled") + "&f.");		
		return true;
	}
	
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onDamage(EntityDamageEvent e) {
		if (GODS.isEmpty()) return;
		if (e.getEntity() instanceof Player && GODS.contains(e.getEntity())) {
			e.setCancelled(true);
			((Player) e.getEntity()).setHealth(((Player) e.getEntity()).getMaxHealth());
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onJoin(PlayerJoinEvent e) {
		GODS.remove(e.getPlayer());
	}
}
