package sh.reece.core;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class Enderchest implements CommandExecutor, Listener {//,TabCompleter,Listener {

	private String Section, Permission, ViewOthers, ModifyOthers;
	private Set<UUID> openEnderChest = new HashSet<>();

	private Main plugin;
	public Enderchest(Main instance) {
		this.plugin = instance;
		Section = "Core.Enderchest";        

		// https://essinfo.xeya.me/permissions.html
		if(plugin.enabledInConfig(Section+".Enabled")) {
			plugin.getCommand("enderchest").setExecutor(this);
			
			// command only
			Permission = plugin.getConfig().getString(Section+".Permission"); // base command	
			ViewOthers = plugin.getConfig().getString(Section+".ViewOthers"); // allows argument

			// event only
			ModifyOthers = plugin.getConfig().getString(Section+".ModifyOthers");
			
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
		}
		
	}
	
	private boolean isEnderSee(Player player){
		if(openEnderChest.contains(player.getUniqueId())) {
			return true;
		}
		return false;
	}
	private void setEnderSee(Player player, boolean value){
		UUID uuid = player.getUniqueId();
		if(value){
			openEnderChest.add(uuid);
		} else {
			openEnderChest.remove(uuid);
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {		
	
		Player target = (Player) sender;
		
		// deny /echest access command
		if (!sender.hasPermission(Permission)) {
			sender.sendMessage(Util.color("&cYou do not have access to &n/" +label+"&c."));
			return true;
		} 

		// if argument & they can view others, allow
		if(args.length >= 1) {
			if(sender.hasPermission(ViewOthers)){
				target = Bukkit.getPlayer(args[0]);	
			} else {
				Util.coloredMessage(sender, "&f[!] &cYou can not view &f" + args[0] + "'s&c enderchest");
			}								
		} 
			
		Player opener = (Player) sender;
		opener.closeInventory();
		// set viewing enderchest for user if they are not themself.
		setEnderSee(opener, !(target.equals(opener)));
		opener.openInventory(target.getEnderChest());		
		return true;
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onInventoryClickEvent(final InventoryClickEvent event){		

		//Player refreshPlayer = null;
		final Inventory top = event.getView().getTopInventory();
		final InventoryType type = top.getType();

		if(type == InventoryType.ENDER_CHEST){
			Player p = (Player) event.getWhoClicked();
			if(isEnderSee(p) && !(p.hasPermission(ModifyOthers))){
				event.setCancelled(true);
				Util.coloredMessage(p, "&f[!] &cYou can not edit their enderchest&f!");
				//refreshPlayer = p;
			}
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onInvClose(final InventoryCloseEvent e){	
		Player refreshPlayer = null;
		final Inventory top = e.getView().getTopInventory();
        final InventoryType type = top.getType();

		if (type == InventoryType.ENDER_CHEST) {
			Player p = ((Player) e.getPlayer());
			setEnderSee(p, false);
            refreshPlayer = p;
		}
		if (refreshPlayer != null) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, refreshPlayer::updateInventory, 1);
        }
	}

	// run this on disable?
	public void closeAllViewedEnderchest() {
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(openEnderChest.contains(p.getUniqueId())){
				p.getOpenInventory().close();
			}						
		}
		openEnderChest.clear();
	}

	
}
