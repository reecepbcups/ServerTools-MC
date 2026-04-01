package sh.reece.GUI;

import sh.reece.tools.AlternateCommandHandler;
import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.chat.Chat;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Tags implements CommandExecutor, Listener { //

	private static Main plugin;
	private FileConfiguration tagsconfig;
	private final String Section;
	private String FILENAME;
	private String InvName;
	private String CustomTagPerm;
	private String CustomTagFormat;
	public static Inventory tagsGUI;
	public int rows, CustomTagMaxLen;

	private String selectedmsg, removedmsg, giveTagCMD;
	//public static LuckPerms luckPerms;
	
	private ConfigUtils ConfigUtils;
	
	public Tags(Main instance) {
		plugin = instance;

		Section = "Chat.Tags";        

		if(plugin.enabledInConfig(Section+".Enabled")) {
			ConfigUtils = plugin.getConfigUtils();

			// vault not instead
			if(!Util.isPluginInstalledOnServer("Vault", "TAGS")) {
				return;
			}
			setupChat();    		

			giveTagCMD  = plugin.getConfig().getString(Section+".giveTagCmd");
			
			FILENAME = "Tags.yml";
			ConfigUtils.createFile(FILENAME);
			tagsconfig = ConfigUtils.getConfigFile(FILENAME);	
			InvName = Util.color("&lTags");

			selectedmsg = plugin.getConfig().getString(Section+".selected");
			removedmsg = plugin.getConfig().getString(Section+".removed");

			// custom tag config info
			CustomTagPerm = plugin.getConfig().getString(Section+".CustomTagPerm");
			CustomTagMaxLen = plugin.getConfig().getInt(Section+".CustomMaxLength");
			CustomTagFormat = plugin.getConfig().getString(Section+".CustomTagFormat");
			
			plugin.getCommand("tags").setExecutor(this);
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
			rows = 5*9;    
			
			
			if(!tagsconfig.contains("Tags")) {
				tagsconfig.set("Tags.Boss", "&8&l<&6BossTag&8&l>");			
				tagsconfig.set("Tags.Spicy", "&8&l<&c&lS&a&lP&c&lI&a&lC&c&lY&8>");
				tagsconfig.set("Tags.OG", "&8&l<&b&lOG&8&l>");
				tagsconfig.set("Tags.Simp", "&8&l<&d&lSIMP&8&l>");
				tagsconfig.set("Tags.Pog", "&8&l<&6&lP&e&lO&6&lG&8&l>");
				tagsconfig.set("Tags.Twitch", "&8&l<&5&lT&d&lW&5&lI&d&lT&5&lC&d&lH&8&l>");
				tagsconfig.set("Tags.YouTube", "&8&l<&c&lYOU&f&lTUBE&8&l>");
				tagsconfig.set("Tags.King", "&8&l<&6&lKing&8&l>");
				tagsconfig.set("Tags.Abuse", "&8&l<&4&lABUSE&8&l>");
				tagsconfig.set("Tags.Grinder", "&8&l<&6&lGRINDER&8&l>");
				tagsconfig.set("Tags.Pay2Win", "&8&l<&2&lPay&a&l2&2&lWin&8&l>");
				tagsconfig.set("Tags.DumDum", "&8&l<&7&oDumDum&8&l>");
				tagsconfig.set("Tags.Beta", "&8&l<&b&oBeta&8&l>");
				tagsconfig.set("Tags.Salty", "&8&l<&f&l&oSalty&8&l>");
				tagsconfig.set("Tags.Fantasy", "&8&l<&d&lFantasy&8&l>");
				tagsconfig.set("Tags.EGirl", "&8&l<&d&lE-Girl&8&l>");
				tagsconfig.set("Tags.Captain", "&8&l<&b&lCaptain&8&l>");
				tagsconfig.set("Tags.CactusGod", "&8&l<&2Cactus&aGod&8&l>");
				tagsconfig.set("Tags.Tryhard", "&8&l<&4&lTryhard&8&l>");
				ConfigUtils.saveConfig(tagsconfig, "Tags.yml");
			}

		} else {
			AlternateCommandHandler.addDisableCommand("tags");
		}
	}

	private boolean setupChat() {
		RegisteredServiceProvider<Chat> rsp = Bukkit.getServer().getServicesManager().getRegistration(Chat.class);
		Main.chat = rsp.getProvider();
		return Main.chat != null;
	}

	public void openTagsForPlayer(Player p) {

		tagsGUI = Bukkit.createInventory(null, rows, InvName);

		int i = 0; // Updates for any new tags every inv open
		Set<String> TAGS = tagsconfig.getConfigurationSection("Tags").getKeys(false);
		
		for(String tag : TAGS) {
			// permision is Tags.tagname
			String perm = "Tags."+tag;
			String format = tagsconfig.getString(perm);
			List<String> lore = new ArrayList<String>();


			lore.add("");
			lore.add("  &7* &f&lPreview: &r" + format);
			lore.add("");

			Material itemmat;
			if(p.hasPermission(perm)) {
				itemmat = Material.NAME_TAG;
				lore.add(ConfigUtils.lang("TAGS_AVAILABLE"));
				lore.add(ConfigUtils.lang("TAGS_CLICK_TO_EQUIP"));
			} else {
				itemmat = Material.BARRIER;
				lore.add(ConfigUtils.lang("TAGS_LOCKED"));
				lore.add(ConfigUtils.lang("TAGS_NO_ACCESS"));
			}	


			createDisplay(tagsGUI, itemmat, i, ConfigUtils.lang("TAG_GUI_FORMAT").replace("%tag%", tag), lore);
			i+=1;
		}
		
		createDisplay(tagsGUI, Material.ANVIL, 40, ConfigUtils.lang("TAG_CLEAR"), new ArrayList<String>());
		p.openInventory(tagsGUI);
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if(args.length==0) {
			openTagsForPlayer((Player) sender);
			return true;
		} 
		
		switch(args[0]){
			case "give":
				
				if(!sender.hasPermission("tools.givetag")) {
					sender.sendMessage(Util.color("&cYou do not have perms to give tags :("));
					return true;
				}
				
				if(!(args.length>=3)) {
					sender.sendMessage("Incorrect Usage: /tags give <player> <tag>");
					return true;
				}
				
				// if player is online now OR they have been on in the past.
				if(Bukkit.getPlayer(args[1]) != null || Bukkit.getOfflinePlayer(args[1]).hasPlayedBefore()) {
					String name = Bukkit.getOfflinePlayer(args[1]).getName();

					giveTagCMD = giveTagCMD.replace("%name%", name);
					giveTagCMD = giveTagCMD.replace("%tag%", args[2]);
					Util.console(giveTagCMD);
					
					if(Bukkit.getPlayer(args[1]).isOnline()) {
						Util.coloredMessage(Bukkit.getPlayer(args[1]), ConfigUtils.lang("TAG_RECEIVED").replace("%tag%", args[2]));
					}					
					sender.sendMessage(Util.color("&a[!] Gave " + args[1] + " the " + args[2] + " tag!"));
					Util.console("lp user "+args[1]+" permission set tags."+args[2]);
				} else {
					sender.sendMessage(Util.color("&cPlayer "+args[1]+" has not played before"));					
				}				
				return true;
				
				
			case "set": // sender.sendMessage(Util.color(""));
				
				if(!sender.hasPermission(CustomTagPerm)) {
					sender.sendMessage(ConfigUtils.lang("TAG_DENY_CUSTOM"));
					return true;
				}
				
				if(!(args.length >= 2)) {
					helpMenu(sender);
					return true;
				}
				
				String Tag = "";
				for (int i = 1; i < args.length; i++) {
					if(i+1 < args.length) {
						Tag += args[i] + " ";
					} else {
						Tag += args[i];
					}
		        }
				
				int ColorUseCount = (Tag.length() - Tag.replace("&", "").length()) * 2;
				if (Tag.length() - ColorUseCount > CustomTagMaxLen) {
					sender.sendMessage(Util.color("&cPlease use less than 20 characters for this tag!"));
					return true;
				}	
				
				addTagToUser((Player) sender, CustomTagFormat.replace("%tag%", Tag)); // /tags set &7test	
				sender.sendMessage(Util.color("&c&o(( Inappropriate tags will result in a punishment ))"));
				return true;
				
			case "clear":
				removeTagFromUser((Player) sender);
				return true;
				
			case "list":
				String tagList = "";
				for(String tagname : tagsconfig.getConfigurationSection("Tags").getKeys(false)){
					tagList += "&f" + tagname + "&7,  ";
				}
				Util.coloredMessage(sender, tagList);
				return true;

			case "create":	
				if(args.length < 3) {
					helpMenu(sender);
					return true;
				}					
				createNewTag((Player) sender, args);
				return true;
				
			default:
				helpMenu(sender);
				return true;
		}
	}

	public void createNewTag(Player p, String[] args) {
			
		if(!p.hasPermission("tools.createtag")) {
			p.sendMessage(Util.color("&cYou do not have perms to create tags :("));
			return;
		}
		
		String name = args[1];
		String tag = Util.argsToSingleString(2, args);
		
		if(tagsconfig.contains("Tags."+name)) {
			Util.coloredMessage(p, "&cThe tag: " + name + " already exist!");
			return;
		}
		
		tagsconfig.set("Tags."+name, tag);
		ConfigUtils.saveConfig(tagsconfig, "Tags.yml");
		Util.coloredMessage(p, "&a[!] Created tag &r" + tag + "&a successfully!");
		
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent event) {
		Player p = (Player) event.getWhoClicked();
		ItemStack clicked = event.getCurrentItem();

		if (event.getInventory() == null || clicked == null || clicked.getType() == Material.AIR) {
			return;
		}	
		
		if(event.getView().getTitle().equalsIgnoreCase(InvName)) {
			String itemName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
			String tag = itemName.substring(4);

			if(itemName.equalsIgnoreCase("CLEAR TAG")) {
				removeTagFromUser(p);
				event.setCancelled(true);
				p.closeInventory();
				return;
			}

			if(p.hasPermission("Tags." + tag)) {
				addTagToUser(p, tagsconfig.getString("Tags."+tag));					
			} 

			p.closeInventory();
			event.setCancelled(true);
		}
	}

	public void helpMenu(CommandSender sender) {
		sender.sendMessage(Util.color("&7&m"+"----------------------------"));
		sender.sendMessage(Util.color("&8- &f/tags &7set <tag>"));
		
		sender.sendMessage(Util.color("&8- &f/tags &7clear"));
		
		sender.sendMessage(Util.color("\n&8- &f/tags &7list"));
		sender.sendMessage(Util.color("&8- &f/tags &7give <player> <tag> &c(admin)"));
		sender.sendMessage(Util.color("&8- &f/tags &7create <name> <tag> &c(admin)"));
		sender.sendMessage(Util.color("&7&m"+"----------------------------"));
	}

	public void addTagToUser(Player p, String tag) {
		Main.chat.setPlayerSuffix(p, " "+tag);
		Util.coloredMessage(p, selectedmsg.replace("%tag%", tag) );	    
	}
	public void removeTagFromUser(Player p) {
		Main.chat.setPlayerSuffix(p, "");
		Util.coloredMessage(p, removedmsg);
	}



	public static void createDisplay(Inventory inv, Material material, int Slot, String name, List<String> list) {
		ArrayList<String> Lore = new ArrayList<String>();

		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(Util.color(name));

		for(String l : list) {
			Lore.add(Util.color(l));
		}

		meta.setLore(Lore);
		item.setItemMeta(meta);

		inv.setItem(Slot, item); 		 
	}



}
