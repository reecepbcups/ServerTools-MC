package sh.reece.events;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class AntiCraft implements CommandExecutor, Listener {

	protected static FileConfiguration storage;	  
	//protected static FileConfiguration config;	  
	private static File f;
	private String Message, bypass, AdminPerm;

	private final Main plugin;
	private ConfigUtils configUtils;
	public AntiCraft(Main instance) {
		plugin = instance;
		configUtils = plugin.getConfigUtils();
		
		final String section = "Events.AntiCraft";
		final String YMLFile = "AntiCraft.yml";
		if (plugin.enabledInConfig(section+".Enabled")) {
			plugin.getCommand("AntiCraft").setExecutor(this);
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
			
			configUtils.createConfig(YMLFile);
			//config = plugin.getConfig();
			storage = configUtils.getConfigFile(YMLFile);
			
			f = new File(plugin.getDataFolder().getAbsolutePath(), YMLFile);

			Message = plugin.getConfig().getString(section+".MSG");
			bypass = plugin.getConfig().getString(section+".Bypass");
			AdminPerm = plugin.getConfig().getString(section+".AdminPerm");
		}

	}

	@EventHandler(ignoreCancelled = true)
	public void onPrepare(PrepareItemCraftEvent e) {
		if (e.getRecipe() == null || e.getRecipe().getResult() == null) {
			return; 
		}
			
		ItemStack item = e.getRecipe().getResult();
		
		if (isBlocked(item) && perms(item, e.getViewers())) {
			e.getInventory().setItem(0, null);
			if (Message != null && !Message.equals("")) {
				for (HumanEntity h : e.getViewers()) {
					h.sendMessage(Util.color(Message.replace("%item%", item.getType().toString().replace("_", " ").toLowerCase())));
				}
			}
		} 
	}

	@EventHandler(ignoreCancelled = true)
	public void onClick(InventoryClickEvent e) {

		if (e.getClickedInventory() == null || e.getClickedInventory().getType() != InventoryType.CRAFTING) {
			return;
		}
		ItemStack item = ((CraftingInventory)e.getClickedInventory()).getResult();
		if (item == null) {
			return;
		}
		if (isBlocked(item) && perms(item, e.getClickedInventory().getViewers())) {
			Main.logging("cancel");
			e.setCancelled(true);
			e.getInventory().setItem(0, null);
		} 
	}

	


	// command
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 0) {
			sender.sendMessage(" ");
			sender.sendMessage(Util.color("&c&lAntiCraft"));
			sender.sendMessage(Util.color("&7Help, Block, Unblock"));
			sender.sendMessage(" ");
			return false;
		} 
		
		if (!(sender instanceof Player)) {
			sender.sendMessage("Player only command.");
			return false;
		} 
		
		if (!sender.hasPermission(AdminPerm)) {
			sender.sendMessage(Util.color("&cNot enough permissions."));
			return false;
		} 
		
		
		if (args.length == 1) {
			ItemStack hand = getItemInHand((Player)sender);
			String msg = "";
			
			if (args[0].equalsIgnoreCase("block")) {
				if (isBlocked(hand)) {
					msg = "&c&lThat item is already blocked!";
				} else {
					add(hand);
					msg = "&c&lYou have blocked " + toConfigString(hand);
				} 
			} 
			
			if (args[0].equalsIgnoreCase("unblock")) {
				if (!isBlocked(hand)) {
					msg = "&c&lThat item is not blocked!";
				} else {
					remove(hand);
					msg = "&c&lYou have un-blocked " + toConfigString(hand);

				} 
			} 
			
			if(msg.length() > 1) {
				sender.sendMessage(" ");
				sender.sendMessage(Util.color(msg));
				sender.sendMessage(" ");
			} else {
				sender.sendMessage(" ");
				sender.sendMessage(Util.color("&e&l/" + label + " block &7- Block crafting of this item."));
				sender.sendMessage(Util.color("&e&l/" + label + " unblock &7- Allow crafting of this item."));
				sender.sendMessage(" ");
			}
			return false;
		} 
		return false;
	}

	public ItemStack getItemInHand(Player arg0) {
		return (ItemStack) arg0.getItemInHand();
	}

	public static List<ItemStack> getBlockedItems() {
		if (storage.getStringList("BlockedCrafting") == null) {
			return new ArrayList<>();
		}
		List<ItemStack> toReturn = new ArrayList<>();
		for (String s : storage.getStringList("BlockedCrafting")) {
			toReturn.add(toItemStack(s));
		}
		return toReturn;
	}

	public static boolean isBlocked(ItemStack arg0) {
		List<ItemStack> items = getBlockedItems();
		if (items.contains(arg0)) {
			return true;
		}
		for (ItemStack item : items) {
			if (item.getType() == arg0.getType() && item.getDurability() == arg0.getDurability()) {
				return true;
			}
		} 
		return false;
	}

	public static void add(ItemStack arg0) {
		List<String> blocked = storage.getStringList("BlockedCrafting");
		if (blocked == null) {
			blocked = new ArrayList<>();
		}
		blocked.add(toConfigString(arg0));
		storage.set("BlockedCrafting", blocked);
		save();
	}

	public static void remove(ItemStack arg0) {
		List<String> blocked = storage.getStringList("BlockedCrafting");
		if (blocked == null) {
			blocked = new ArrayList<>();
		}
		blocked.remove(toConfigString(arg0));
		storage.set("BlockedCrafting", blocked);
		save();
	}
	
	private boolean perms(ItemStack arg0, List<HumanEntity> arg1) {
		for (HumanEntity h : arg1) {
			for (String s : getPermissions(arg0)) {
				if (h.hasPermission(s)) {
					return false;
				}
			} 
		} 
		return true;
	}

	private List<String> getPermissions(ItemStack arg0) {
		List<String> p = new ArrayList<>();
		if (arg0.getDurability() == 0) {
			p.add(bypass + "" + arg0.getType().name().toLowerCase().replace("legacy_", ""));
		}
		p.add(bypass + "" + arg0.getType().name().toLowerCase().replace("legacy_", "") + ":" + arg0.getDurability());
		return p;
	}

	private static void save() {
		try {
			storage.save(f);
		} catch (IOException iOException) {}
	}

	public static String toConfigString(ItemStack arg0) {
		return String.valueOf(arg0.getType().name().toLowerCase(Locale.ENGLISH)) + ((arg0.getDurability() != 0) ? ("-" + arg0.getDurability()) : "");
	}

	public static ItemStack toItemStack(String arg0) {
		arg0 = arg0.toUpperCase();
		short data = 0;
		if (arg0.contains("-") && arg0.split("-")[1] != null && !arg0.split("-")[1].isEmpty()) {
			data = Short.valueOf(arg0.split("-")[1]).shortValue();
		}
		return new ItemStack(Material.getMaterial(arg0.contains("-") ? arg0.split("-")[0] : arg0), 1, data);
	}

	public static void console(String arg0, Object... arg1) {
		Main.logging("[AntiCraft] " + String.format(arg0.replace("\\n", "\n"), arg1));
	}


}
