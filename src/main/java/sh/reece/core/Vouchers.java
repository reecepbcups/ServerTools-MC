package sh.reece.core;

import sh.reece.utiltools.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import sh.reece.tools.AlternateCommandHandler;
import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;

public class Vouchers implements Listener, CommandExecutor, TabCompleter {

	private static Main plugin;
	private FileConfiguration config;
	private final String Section;
	private String RedeemMessage = null;
	public static Set<String> voucherKeys;
	private static Inventory GUI;
	// Bundle / Vouchers plugin
	private String InvName;
	private Boolean Glowing;

	// - '  &d&l%player% &7has redeemed their &5&lDragon Reclaim'

// CONFIG


	// [NAME: {itemstack, commands, VoucherID}]
	private static final HashMap<String, List<Object>> VOUCHERS = new HashMap<String, List<Object>>();
	//private static List<Inventory> previewVouchersGUIs = new ArrayList<Inventory>();
	//private static HashMap<String, Integer> playerPreviewPage = new HashMap<String, Integer>();
	private ConfigUtils configUtils;

	public Vouchers(Main instance) {
        plugin = instance;

        Section = "Vouchers";
        if(plugin.getConfigUtils().enabledInConfig(Section+".Enabled")) {
			configUtils = plugin.getConfigUtils();

        	configUtils.createConfig("Vouchers.yml");
        	config = configUtils.getConfigFile("Vouchers.yml");

        	if(!(config.getKeys(false).size() > 0)) {
        		Util.consoleMSG("&c[ServerTool-Vouchers] No Vouchers found in the Vouchers.yml!");
        		return;
        	}

        	voucherKeys = config.getKeys(false);
        	Glowing = config.getBoolean(Section+".Options.Glowing");

        	RedeemMessage = Util.color(plugin.getConfig().getString(Section+".Options.RedeemMessage"));
			if(RedeemMessage == null) {
				RedeemMessage = "&a&l[+]&a You redeemed the %voucher% &7(%voucherid%)";
			}

        	createPreviewGUI();

        	plugin.getCommand("voucher").setExecutor(this);
        	plugin.getCommand("voucher").setTabCompleter(this);
    		Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    	} else {
			AlternateCommandHandler.addDisableCommand("voucher");
		}
	}

	public void createPreviewGUI() {

		InvName = configUtils.lang("VOUCHER_GUI_NAME");
    	GUI = Bukkit.createInventory(null, 54, InvName);

    	//int slot = 0;
    	ItemStack VoucherItem;
    	List<Object> voucherValues;

    	int slot = 0;
    	for(String voucher : voucherKeys) {

    		// if we are overstepping where we can place, stop it
    		if(slot >= GUI.getSize()) {
    			break;
    		}


    		VoucherItem = createItem(voucher, 1);

    		GUI.setItem(slot, VoucherItem);
    		slot++;

    		voucherValues = new ArrayList<Object>();
    		voucherValues.add(VoucherItem);
    		//voucherValues.add(lore);
    		voucherValues.add(config.getStringList(voucher+".Commands"));
    		voucherValues.add(voucher);
    		VOUCHERS.put(Util.color(config.getString(voucher+".Name")), voucherValues);
    	}

	}

	public ItemStack nextPageFeather() {
		ItemStack nextPage = new ItemStack(Material.FEATHER);
		ItemMeta im = nextPage.getItemMeta();
		im.setDisplayName(Util.color("&bNext Page>>"));
		nextPage.setItemMeta(im);
		return nextPage;
	}

	public void sendHelpMenu(String cmd, Player p) {
		// add codes?
		// voucher redeem SOMECODE -> Run commands
		Util.coloredMessage(p, " &f- &b/"+cmd+" &fgive <player> <Voucher> [amount]");
		Util.coloredMessage(p, " &f- &b/"+cmd+" &fgiveall <Voucher>");
		Util.coloredMessage(p, " &f- &b/"+cmd+" &flist / GUI");
	}

	@SuppressWarnings("deprecation")
	public ItemStack createItem(String voucherID, int amount){

		if(!config.contains(voucherID)) {
			Util.consoleMSG("No VoucherID in config named: " + voucherID);
			return null;
		}

		String displayName = Util.color(config.getString(voucherID+".Name"));

		String newItem = config.getString(voucherID+".Item").split(":")[0];
		if(newItem.equalsIgnoreCase("sunflower")){ newItem = "DOUBLE_PLANT"; }
		if(newItem.equalsIgnoreCase("experience_bottle")){ newItem = "EXP_BOTTLE"; }

		Material material = Material.getMaterial(newItem);
		if(material == null) {
			Util.consoleMSG("[vouchers] No material found for: " + newItem + ". Setting as anm amethest block");
			material = Material.AMETHYST_BLOCK;
		}


		List<String> lore = config.getStringList(voucherID+".Lore");
		lore = Util.color(lore);

		int shortValue = 0;
		if(config.getString(voucherID+".Item").contains(":")) {
			shortValue = Integer.valueOf(config.getString(voucherID+".Item").split(":")[1]);
		}
        ItemStack item = new ItemStack(material, amount, (short) shortValue);

        ItemMeta meta = item.getItemMeta();
        if(Glowing) {
        	meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        meta.setDisplayName(displayName);
        meta.setLore(lore);
        item.setItemMeta(meta);

        if(Glowing) {
        	item.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }

        return item;
    }

	@EventHandler(ignoreCancelled = true)
	public void playerClickedOnVoucher(PlayerInteractEvent e) {
		// click of voucher	if in keys

		if(e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {

			//Util.coloredBroadcast("clicked air / block");
			if(!e.hasItem()) {
				return;
			}

			ItemStack item = e.getItem();

			if(!item.hasItemMeta()) {
				return;
			}

			if(!item.getItemMeta().hasDisplayName()) {
				return;
			}
			if(!item.getItemMeta().hasLore()) {
				return;
			}


			String displayName = Util.color(item.getItemMeta().getDisplayName());
			//Util.coloredBroadcast("has displayname " + displayName);

			if(!VOUCHERS.containsKey(displayName)) {
				//Util.coloredBroadcast("vopuchers contains " + displayName);
				return;
			}

			Player p = e.getPlayer();
			if(item.isSimilar((ItemStack) VOUCHERS.get(displayName).get(0))) {

				@SuppressWarnings("unchecked") List<String> commands = (List<String>) VOUCHERS.get(displayName).get(1);
				if(commands != null) {

					Util.removeItemFromPlayer(p, item, 1);

					String voucherID = VOUCHERS.get(displayName).get(2).toString();
					for(String cmd : commands) {

						cmd = cmd.replace("%player%", p.getName())
								.replace("%voucher%", voucherID);
						Util.consoleMSG(cmd);

						Util.console(cmd);
					}

					if(RedeemMessage.length() > 0) {
						Util.coloredMessage(p,
							RedeemMessage.replace("%voucher%", displayName)
							.replace("%voucherid%", voucherID));
					}
				}
			}
		}



	}



	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if(sender instanceof ConsoleCommandSender) {
			switch (args[0]) {
			case "give":
				// /voucher give Reecepbcups <voucher> [amount]
				if(args.length < 3) {
					sender.sendMessage("&c/voucher give <player> <voucher> [amount]");
					return true;
				}
				int amount = 1;
				if(Util.isInt(args[3])) {
					amount = Integer.valueOf(args[3]);
				}
				givePlayerVoucher(args[1], args[2], amount);

			default:
				break;
			}
			return true;
		}



		Player p = (Player) sender;

		if (args.length == 0) {
			sendHelpMenu(label, p);
			return true;
		}

		if(!p.hasPermission("voucher.admin")){
			Util.coloredMessage(p, "&cNo permissions to alter vouchers! &7&0[voucher.admin]");
			return true;
		}

		switch(args[0]){
			case "give":
				// /voucher give Reecepbcups <voucher> [amount]
				if(args.length < 3) {
					Util.coloredMessage(p, "&c/voucher give <player> <Voucher> [amount]");
					return true;
				}


				int amount = 1;
				if(args.length >= 4) {
					if(Util.isInt(args[3])) {
						amount = Integer.valueOf(args[3]);
					}
				}

				givePlayerVoucher(args[1], args[2], amount);
				return true;

			case "giveall":
				// /voucher giveall <voucher>
				ItemStack item = createItem(args[1], 1);
				Bukkit.getOnlinePlayers().stream().forEach(t -> t.getInventory().addItem(item));
				return true;

			case "list":
				// /voucher giveall <voucher>
				Util.coloredMessage(p, voucherKeys.toString());
				return true;

			case "gui":
				// /voucher gui
				// sets players
				p.openInventory(GUI);
				return true;

			default:
				sendHelpMenu(label, p);
				return true;
		}
	}

	public void givePlayerVoucher(String Target, String Voucher, Integer amount) {
		Player target = Bukkit.getPlayer(Target);
		if(target == null) {
			Util.consoleMSG(Target + " is not online to give a voucher too");
			return;
		}

		// add check here if voucher is real?
		target.getInventory().addItem(createItem(Voucher, amount));
	}


	@EventHandler(ignoreCancelled = true) // gives itemstack on click
	public void onInventoryClick(InventoryClickEvent event) {
		ItemStack clicked = event.getCurrentItem();

		if(event.getView().getTitle().equalsIgnoreCase(InvName)) {
			if(clicked == null) {
				return;
			}
			event.setCancelled(true);
			event.getWhoClicked().getInventory().addItem(clicked);
		}
	}


	private static final List<String> possibleArugments = Arrays.asList("give", "giveall", "list");
	private final List<String> result = new ArrayList<String>();
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		result.clear();

		// voucher <tab>
		if(args.length == 1) {
			for(String a : possibleArugments) {
				if(a.toLowerCase().startsWith(args[0].toLowerCase())) {
					result.add(a);
				}
			}
			return result;
		}

		///voucher give Reecepbcups <tab>
		if(args.length == 3 && args[0].equalsIgnoreCase("give")) {
			for(String a : voucherKeys) {
				if(a.toLowerCase().startsWith(args[2].toLowerCase())) {
					result.add(a);
				}
			}
			return result;
		}

		///voucher giveall <tab>
		if(args.length == 2 && args[0].equalsIgnoreCase("giveall")) {
			for(String a : voucherKeys) {
				if(a.toLowerCase().startsWith(args[1].toLowerCase())) {
					result.add(a);
				}
			}
			return result;
		}



		return null;
	}



}
