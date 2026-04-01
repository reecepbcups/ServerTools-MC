package sh.reece.GUI;

import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class NameColor implements Listener, CommandExecutor {

	public String command, perm;
	public static String InvName;
	//public static String FILENAME;
	public static Inventory ColorINV;
	//public static FileConfiguration config;

	public Random rand = new Random();
	public static Boolean isEnabled;

	public static HashMap<String, String> NameColorHash = new HashMap<String, String>();

	// 2, 12, 15 - REMOVED= pink1, brown, black
	public static List<String> Values = Arrays.asList("&fWhite", "&6Orange", "&dPink", "&bAqua","&eYellow","&aLime", "&dPink","&8Dark Gray","&7Gray", "&3Cyan","&5Purple","&1Blue","&fBrown","&2Green","&cRed","&0Black");
	public static List<String> EMPTY_LORE = new ArrayList<String>();
	private ConfigUtils configUtils;

	private static Map<String, Material> panelColorMap = new HashMap<String, Material>() {{
		put("&f", Material.WHITE_STAINED_GLASS_PANE);
		put("&6", Material.ORANGE_STAINED_GLASS_PANE);
		put("&d", Material.PINK_STAINED_GLASS_PANE);
		put("&b", Material.LIGHT_BLUE_STAINED_GLASS_PANE);
		put("&e", Material.YELLOW_STAINED_GLASS_PANE);
		put("&a", Material.LIME_STAINED_GLASS_PANE);
		put("&8", Material.GRAY_STAINED_GLASS_PANE);
		put("&7", Material.LIGHT_GRAY_STAINED_GLASS_PANE);
		put("&3", Material.CYAN_STAINED_GLASS_PANE);
		put("&5", Material.PURPLE_STAINED_GLASS_PANE);
		put("&1", Material.BLUE_STAINED_GLASS_PANE);
		put("&2", Material.GREEN_STAINED_GLASS_PANE);
		put("&c", Material.RED_STAINED_GLASS_PANE);
		put("&4", Material.RED_STAINED_GLASS_PANE);
		put("&0", Material.BLACK_STAINED_GLASS_PANE);
	}};
	
	public static Main plugin;
	public NameColor(Main instance) {
		plugin = instance;

		isEnabled = false;

		// v - also in the main class
		if (plugin.enabledInConfig("Chat.NameColor.Enabled")) {
			isEnabled = true;

			configUtils = plugin.getConfigUtils();
			// plugins/ServerTools/DATA
//			configUtils.createDirectory("DATA");
//			FILENAME = File.separator + "DATA" + File.separator + "NameColor.yml";
//			configUtils.createFile(FILENAME);
//			config = configUtils.getConfigFile(FILENAME);	

			command = "/namecolor";
			perm = "Namecolor.";	

			InvName = configUtils.lang("NAMECOLOR_GUI");

			// Creates
			initCreateInv();

			//loadToMemory();
			
			plugin.getCommand("namecolor").setExecutor(this);
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		((Player) sender).openInventory(ColorINV);
		return true;
	}

	public void initCreateInv() {
		ColorINV = Bukkit.createInventory(null, 2*9, InvName); 

		int loop = 0;
		for(int i: Arrays.asList(0,1,3,4,5,6,7,8,9,10,11,13,14)) {

			String name = Values.get(i);
			String color = name.substring(0,2);

			List<String> lore = new ArrayList<String>();
			lore.add("");
			lore.add(color + configUtils.lang("NAMECOLOR_INFO"));
			lore.add(color + configUtils.lang("NAMECOLOR_COLOR") + name.substring(2,name.length()));
			lore.add(color + configUtils.lang("NAMECOLOR_ACCESS"));
			lore.add("");
			lore.add(configUtils.lang("NAMECOLOR_SELECT"));

			createDisplay(ColorINV, new ItemStack(panelColorMap.get(color), 1), 
					loop, color+"&l[!] "+ name, lore);

			loop+=1;
		}
		//createDisplay(ColorINV, new ItemStack( Material.EXP_BOTTLE), loop, "&fR&ea&6i&bn&2b&4o&7w", EMPTY_LORE);
		
		// light gray - fixes from people putting in items
		// create a for loop from 13 to 17
		for(int slot = 13; slot < 18; slot++) {
			createDisplay(ColorINV, new ItemStack( Material.LIGHT_GRAY_STAINED_GLASS_PANE), slot, " ", EMPTY_LORE);
		}		
	}

	public static String getColor(String uuid) {
		if(NameColorHash.containsKey(uuid)) {
			return NameColorHash.get(uuid);
		} 		
		return null;		
	}
	
	public void setColor(Player p, String color, String msg) {

		// removes the "[!] " = 4 chars
		if(!color.equalsIgnoreCase("Rainbow")) {
			color = color.substring(4,color.length());
		}
			
		String uuid = p.getUniqueId().toString();
		String colorcode = "";

		switch (color) {
		case "White": colorcode="&f"; break;		
		case "Orange": colorcode="&6"; break;			
		case "Pink": colorcode="&d"; break;
		case "Aqua": colorcode="&b"; break;
		case "Yellow": colorcode="&e"; break;
		case "Lime": colorcode="&a"; break;
		case "Dark Gray": colorcode="&8"; break;
		case "Gray": colorcode="&7"; break;
		case "Cyan": colorcode="&3"; break;
		case "Purple": colorcode="&5"; break;
		case "Blue": colorcode="&1"; break;
		case "Green": colorcode="&2"; break;
		case "Red": colorcode="&c"; break;
		}		
		
		NameColorHash.put(uuid, colorcode);
		Util.coloredMessage(p, configUtils.lang("NAMECOLOR_SET").replace("%color%", colorcode+color));
	}

	@EventHandler(ignoreCancelled = true)
	public void playerColoredNameEvent(AsyncPlayerChatEvent e) {
		if(isEnabled) {
			Player p = e.getPlayer();
			String color = getColor(e.getPlayer().getUniqueId().toString());
			
			if(color != null) {
				String name = p.getDisplayName();
				if(p.getCustomName() != null) {
					name = p.getCustomName();
				}				
				p.setDisplayName(Util.color(color+name));
			} 
		}			
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent event) {
		if(event.getView().getTitle().equalsIgnoreCase(InvName)) {

			ItemStack clicked = event.getCurrentItem();
			if(clicked == null || !clicked.hasItemMeta()) {return;}

			Player p = (Player) event.getWhoClicked();
			String DisplayName = clicked.getItemMeta().getDisplayName();

			if(DisplayName.equalsIgnoreCase(" ")) {
				event.setCancelled(true); // if its the last inv areas where there is no colors set
				return;
			}
			
			String nameperm = perm + org.bukkit.ChatColor.stripColor(DisplayName);
			if(p.hasPermission(nameperm)){
				setColor(p, nameperm.split(perm)[1], "");
				
			} else {
				p.sendMessage(Util.color("&4&l[!]&c You do not have permission to use &N" + nameperm));
			}

			p.closeInventory();
			event.setCancelled(true);
		}
	}

	public static void createDisplay(Inventory inv, ItemStack itemStack, int Slot, String name, List<String> list) { //

		ItemStack item = itemStack;
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(Util.color(name));			

		ArrayList<String> Lore = new ArrayList<String>();
		if(list.size() > 0) {
			for(String l : list) {
				Lore.add(Util.color(l));
			}
		}
		meta.setLore(Lore);		
		
		item.setItemMeta(meta);

		inv.setItem(Slot, item); 

	}	
}