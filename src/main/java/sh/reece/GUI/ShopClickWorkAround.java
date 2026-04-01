package sh.reece.GUI;


import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class ShopClickWorkAround implements Listener {

	private static Main plugin;
	//private static List<String> values = new ArrayList<String>();
	private static String ShopGUIMenuName;
	private FileConfiguration Shop;
	private String shopPlugin;
	
	public ShopClickWorkAround(Main instance) {
		plugin = instance;
			
		if (plugin.enabledInConfig("ShopWorkAround.Enabled")) {
			
			shopPlugin = plugin.getConfig().getString("ShopWorkAround.plugin");
			try {
				Bukkit.getServer().getPluginManager().getPlugin(shopPlugin).getConfig();
			} catch (Exception e) {
				Util.consoleMSG("&e[ServerTools] &cShop plugin &n" + shopPlugin + "&c not found for \"ShopWorkAround\"");
				return;
			}
			
			
			
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);			
			Shop = Bukkit.getServer().getPluginManager().getPlugin(plugin.getConfig().getString("ShopWorkAround.plugin")).getConfig();
			ShopGUIMenuName = Util.color(Shop.getString(plugin.getConfig().getString("ShopWorkAround.MenuNameInConfig")));

//			for(String key : Shop.getConfigurationSection(plugin.getConfig().getString("ShopWorkaround.KeysofItemsInConfig")).getKeys(false)) {
//				//String mat = s.getString("shopMenuItems." + key + ".item.material");
//				String name = Shop.getString(plugin.getConfig().getString("ShopWorkaround.NameOfItem").replace("%key%", key)); 
//				values.add(name);
//			}
		}
		
		
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onInvClick(InventoryClickEvent e) {
		
		if(e.getCurrentItem() == null) {
			return;
		}
		
//		if(e.getInventory() instanceof PlayerInventory) {
//			e.getWhoClicked().sendMessage("Clicked in your own inv");
//			return;
//		}
		
		String inv_name = Util.color(e.getView().getTitle());
		
		String InvName = ShopGUIMenuName;
		
		
		if(ShopGUIMenuName.equalsIgnoreCase(inv_name)) {
			
			if(!e.getCurrentItem().hasItemMeta()) {
				return;
			}
			
			String ITEMCLICKED = Util.color(e.getCurrentItem().getItemMeta().getDisplayName());
			Player p = (Player) e.getWhoClicked();
			
			if(plugin.getConfig().getString("ShopWorkAround.DEBUG").equalsIgnoreCase("true")) {
				p.sendMessage(" ");
				p.sendMessage("[ShopWorkaround] InvName: " + InvName);
				p.sendMessage("       -->   itemClicked: " +ITEMCLICKED);
			}
			
			
			ConfigurationSection Items =  plugin.getConfig().getConfigurationSection("ShopWorkAround.RemappedClicks"); // 1, 2, 3, 3
			
			//Bukkit.broadcast(Items.getKeys(false).toString(), "");
			
			for(String key : Items.getKeys(false)) {
				
				if(plugin.getConfig().getString("ShopWorkAround.DEBUG").equalsIgnoreCase("true")) {
					p.sendMessage("");
					p.sendMessage("[TOOLS:ShopPatch] ItemsToRemapClicks: " + Util.color(Items.getString(key + ".name")));
					p.sendMessage(" --> CMD: " + Util.color(Items.getString(key + ".command")));
				}
				
				if(ITEMCLICKED.equalsIgnoreCase(Util.color(Items.getString(key + ".name")))) {
					e.setCancelled(true);
					
					if(Items.getString(key + ".CloseInvBeforeCommand").equalsIgnoreCase("true")) {
						p.closeInventory();	
					}
					
					String command = Items.getString(key + ".command");
					
//					if(Items.getString(key+".MakePlayerSayCommand").equalsIgnoreCase("true")) {
//						p.sendMessage("/" + command);
//						Util.console("sudo ");
//						return;
//					} 
					
					p.performCommand(command);
					
							
					
				}
			}
			
		}
		
		
	}
	
	
	
	
	
}
