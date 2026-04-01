package sh.reece.disabled;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class DisableWorldGuardGlitchBuilding implements Listener {

	private static Main plugin;
	private Inventory StopGUI;
	private ItemStack block1;
	private ItemMeta block1Meta;
	
	private String title;
	
	public DisableWorldGuardGlitchBuilding(Main instance) {
        plugin = instance;
        
        if (plugin.enabledInConfig("Disabled.DisableWorldGuardGlitchBuilding.Enabled")) {
    		Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    		
    		title = Util.color("&c&lStop blockglitching");
    		
    		StopGUI = Bukkit.createInventory(null, 27, title);    	    	
    		
    		block1 = new ItemStack(Material.DIAMOND_BLOCK, 1);
			block1Meta = block1.getItemMeta();
			block1Meta.setDisplayName(title);
			
			ArrayList<String> lore = new ArrayList<>();
			block1Meta.setLore(lore);
			block1.setItemMeta(block1Meta);
			lore.add(Util.color("&e&nIt aint cool!"));
			StopGUI.setItem(13, block1);
    	}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		
		if ((!event.canBuild() || event.isCancelled())) {
			
			if(event.getPlayer().hasPermission("blockglitchplace.bypass")){
				return;
			}
			
			Player player = event.getPlayer();					
			
			player.teleport(player.getLocation());
			player.setVelocity(new Vector(0, -1, 0));
	
			player.openInventory(StopGUI);
			//player.sendMessage(Util.color("You attempted to block glitch!"));
		} 
	}
	
// idk maybe I combine eventually??
//	@EventHandler
//	public void movePlace(Event e) {
//		if(e instanceof PlayerMoveEvent) {
//			
//		}
//		if(e instanceof BlockPlaceEvent) {
//			
//		}
//	}
	
	

	@EventHandler(ignoreCancelled = true)
	public void onClick(InventoryClickEvent e) {
		if (e.getView().getTitle().equals(title))
			e.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onInventoryClick(InventoryDragEvent e) {
		if (e.getView().getTitle().equals(title))
			e.setCancelled(true); 
	}
	
	
	
}
