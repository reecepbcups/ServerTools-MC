package sh.reece.GUI;

import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class ChatColor implements Listener, CommandExecutor {

	public String command, perm;
	public static String InvName;
	public static String FILENAME;
	public static Inventory ColorINV;
	public static FileConfiguration config;

	public Random rand = new Random();
	public static Boolean isEnabled;

	public static HashMap<String, String> ChatColorHash = new HashMap<String, String>();

	// 2, 12, 15 - REMOVED= pink1, brown, black
	public static List<String> Values = Arrays.asList("&fWhite", "&6Orange", "&dPink", "&bAqua","&eYellow","&aLime", "&dPink","&8Dark Gray","&7Gray", "&3Cyan","&5Purple","&1Blue","&fBrown","&2Green","&cRed","&0Black");
	public static List<String> EMPTY_LORE = new ArrayList<String>();
	public static List<String> RainbowColors;

	private ConfigUtils configUtils;
	private static Main plugin;

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


	public ChatColor(Main instance) {
		plugin = instance;

		isEnabled = false;

		// v - also in the main class
		if (plugin.enabledInConfig("Chat.ChatColor.Enabled")) {
			isEnabled = true;

			configUtils = plugin.getConfigUtils();

			// plugins/ServerTools/DATA
			configUtils.createDirectory("DATA");
			FILENAME = File.separator + "DATA" + File.separator + "ChatColor.yml";
			configUtils.createFile(FILENAME);
			config = configUtils.getConfigFile(FILENAME);	

			command = "/chatcolor";
			perm = "Chatcolor.";	

			InvName = configUtils.lang("CHATCOLOR_GUI");
			RainbowColors = plugin.getConfig().getStringList("Chat.ChatColor.RainbowColors");
			
			// Creates
			initCreateInv();

			loadToMemory();
			
			plugin.getCommand("chatcolor").setExecutor(this);
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player p = (Player) sender;

		p.openInventory(ColorINV);
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
			lore.add(color + configUtils.lang("CHATCOLOR_INFO"));
			lore.add(color + configUtils.lang("CHATCOLOR_COLOR") + name.substring(2,name.length()));
			lore.add(color + configUtils.lang("CHATCOLOR_ACCESS"));
			lore.add("");
			lore.add(configUtils.lang("CHATCOLOR_SELECT"));

			createDisplay(ColorINV, new ItemStack(panelColorMap.get(color), 1), loop, color+"&l[!] "+ name, lore);

			loop+=1;
		}
		createDisplay(ColorINV, new ItemStack( Material.EXPERIENCE_BOTTLE), loop, configUtils.lang("CHATCOLOR_RAINBOW"), EMPTY_LORE);
		
		// light gray - fixes from people putting in items
		for(int slot = 14; slot < 18; slot++) {
			createDisplay(ColorINV, new ItemStack( Material.LIGHT_GRAY_STAINED_GLASS_PANE), slot, " ", EMPTY_LORE);
		}	
		
	}

	public static String getColor(String uuid) {
		if(ChatColorHash.containsKey(uuid)) {
			return ChatColorHash.get(uuid);
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
		case "Rainbow": 
			rainbowFormat(msg);
			colorcode="Rainbow";
			break;
		}		
		//config.set("Data." + uuid, colorcode) && configUtils.saveConfig(config, FILENAME);

		ChatColorHash.put(uuid, colorcode);

		String colorFormat = colorcode + color;		
		if(colorcode.equalsIgnoreCase("Rainbow")) {
			colorFormat = rainbowFormat(color);
		}		
		Util.coloredMessage(p, configUtils.lang("CHATCOLOR_SET").replace("%color%", colorFormat));

	}
	
	//Rainbow
	public String rainbowFormat(String msg) {
		String _final = "";

		for(String l : msg.split("")) { // hey!

			if(l != " ") {
				_final += RainbowColors.get(rand.nextInt(RainbowColors.size())) + l;
			} else {
				_final += " ";
			}		
		}
		return _final;

	}
	
	//loads Data to memory
	public void loadToMemory() {		
		if(config.getConfigurationSection("Data") != null) {
			for(String uuid : config.getConfigurationSection("Data").getKeys(false)) {
				ChatColorHash.put(uuid, config.getString("Data."+uuid));
			}
		}
	}
	
	//Saves Chat Color To File
	public void saveChatColorToFile() {		
		if(isEnabled) {			
			if(ChatColorHash.keySet().size() > 0) {
				for(String uuid : ChatColorHash.keySet()) {
					config.set("Data." + uuid, ChatColorHash.get(uuid));
				}
				configUtils.saveConfig(config, FILENAME);
			}					
		}				
	}

	@EventHandler(ignoreCancelled = true)
	public void playerColoredChatEvent(AsyncPlayerChatEvent e) {
		String color = getColor(e.getPlayer().getUniqueId().toString());
		
		if(color != null) {
			String OrginMsg = e.getMessage();
			String msg = "";
			
			if(color.equalsIgnoreCase("Rainbow")) {
				msg = rainbowFormat(OrginMsg);				
			} else {
				msg = color + OrginMsg;
			}
			e.setMessage(Util.color(msg));
		} 
	}
	
	//Checks for Clicks
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
			
			String chatpermission = perm + org.bukkit.ChatColor.stripColor(DisplayName);
			if(p.hasPermission(chatpermission)){
				setColor(p, chatpermission.split(perm)[1], "");
			} else {
				p.sendMessage(Util.color("&4&l[!]&c You do not have permission to use &N" + chatpermission));
			}

			p.closeInventory();
			event.setCancelled(true);
		}
	}
	
	//Creates Display
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
