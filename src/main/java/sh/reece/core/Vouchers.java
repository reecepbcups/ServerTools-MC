package sh.reece.core;

import sh.reece.utiltools.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import sh.reece.tools.AlternateCommandHandler;
import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.utiltools.Schedulers;

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
	// tagged vouchers are found by id; untagged ones from older versions by display name
	private static final NamespacedKey VOUCHER_KEY = new NamespacedKey("servertools", "voucher");
	private static final HashMap<String, List<Object>> VOUCHERS_BY_ID = new HashMap<String, List<Object>>();
	// right click fires constantly, so anything not made of a voucher material is dropped before reading meta
	private static final Set<Material> VOUCHER_MATERIALS = EnumSet.noneOf(Material.class);
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
    	GUI = Bukkit.createInventory(new GuiHolder(), 54, InvName);
    	// rebuilt on reload, so drop vouchers that left the config
    	VOUCHERS.clear();
    	VOUCHERS_BY_ID.clear();
    	VOUCHER_MATERIALS.clear();

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
    		// what vouchers given out before the tag existed look like, to match them just as strictly
    		ItemStack legacy = VoucherItem.clone();
    		ItemMeta legacyMeta = legacy.getItemMeta();
    		legacyMeta.getPersistentDataContainer().remove(VOUCHER_KEY);
    		legacy.setItemMeta(legacyMeta);
    		voucherValues.add(legacy);
    		VOUCHERS.put(Util.color(config.getString(voucher+".Name")), voucherValues);
    		VOUCHERS_BY_ID.put(voucher, voucherValues);
    		VOUCHER_MATERIALS.add(VoucherItem.getType());
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
        meta.getPersistentDataContainer().set(VOUCHER_KEY, PersistentDataType.STRING, voucherID);
        item.setItemMeta(meta);

        if(Glowing) {
        	item.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
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

			if(!VOUCHER_MATERIALS.contains(item.getType()) || !item.hasItemMeta()) {
				return;
			}

			// read only view, no meta copy
			String taggedID = item.getPersistentDataContainer().get(VOUCHER_KEY, PersistentDataType.STRING);
			List<Object> voucher;
			if(taggedID != null) {
				voucher = VOUCHERS_BY_ID.get(taggedID);
				// the tag alone isn't proof, it can be copied onto another item. Must match the real voucher exactly
				if(voucher != null && !item.isSimilar((ItemStack) voucher.get(0))) {
					voucher = null;
				}
			} else {
				ItemMeta meta = item.getItemMeta();
				if(!meta.hasDisplayName() || !meta.hasLore()) {
					return;
				}
				voucher = VOUCHERS.get(meta.getDisplayName());
				// material, name, lore, enchants, flags: everything but the tag older vouchers don't have
				if(voucher != null && !item.isSimilar((ItemStack) voucher.get(3))) {
					voucher = null;
				}
			}
			if(voucher == null) {
				return;
			}

			// vouchers are never placed, even when redeeming works (ENDER_CHEST etc.)
			e.setCancelled(true);

			@SuppressWarnings("unchecked") List<String> commands = (List<String>) voucher.get(1);
			if(commands == null) {
				return;
			}

			Player p = e.getPlayer();
			String voucherID = voucher.get(2).toString();
			if(!canRedeem(p, voucherID)) {
				return;
			}

			// take it from the hand that clicked. Clearing the main hand let an offhand voucher be reused forever
			EquipmentSlot hand = e.getHand() == null ? EquipmentSlot.HAND : e.getHand();
			p.getInventory().setItem(hand, item.getAmount() > 1 ? item.asQuantity(item.getAmount() - 1) : null);

			for(String cmd : commands) {

				cmd = cmd.replace("%player%", p.getName())
						.replace("%voucher%", voucherID);
				Util.consoleMSG(cmd);

				Util.console(cmd);
			}

			if(RedeemMessage.length() > 0) {
				String displayName = ((ItemStack) voucher.get(0)).getItemMeta().getDisplayName();
				Util.coloredMessage(p,
					RedeemMessage.replace("%voucher%", displayName)
					.replace("%voucherid%", voucherID));
			}
		}



	}



	// Usage stored in player PDC so cooldowns and limits survive restarts.
	private boolean canRedeem(Player p, String voucherID) {
		String perm = config.getString(voucherID+".Permission");
		if(perm != null && !perm.isEmpty() && !p.hasPermission(perm)) {
			Util.coloredMessage(p, configUtils.lang("VOUCHER_NO_PERMISSION").replace("%permission%", perm));
			return false;
		}

		String safeID = voucherID.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
		PersistentDataContainer pdc = p.getPersistentDataContainer();
		NamespacedKey usesKey = new NamespacedKey("servertools", "voucher_uses_" + safeID);
		NamespacedKey lastKey = new NamespacedKey("servertools", "voucher_last_" + safeID);

		int limit = config.getInt(voucherID+".Limit", 0);
		int uses = pdc.getOrDefault(usesKey, PersistentDataType.INTEGER, 0);
		if(limit > 0 && uses >= limit) {
			Util.coloredMessage(p, configUtils.lang("VOUCHER_LIMIT_REACHED").replace("%limit%", String.valueOf(limit)));
			return false;
		}

		long cooldownMs = config.getLong(voucherID+".Cooldown", 0) * 1000L;
		long now = System.currentTimeMillis();
		long remaining = pdc.getOrDefault(lastKey, PersistentDataType.LONG, 0L) + cooldownMs - now;
		if(cooldownMs > 0 && remaining > 0) {
			Util.coloredMessage(p, configUtils.lang("VOUCHER_ON_COOLDOWN").replace("%seconds%", String.valueOf((remaining + 999) / 1000)));
			return false;
		}

		if(limit > 0) {
			pdc.set(usesKey, PersistentDataType.INTEGER, uses + 1);
		}
		if(cooldownMs > 0) {
			pdc.set(lastKey, PersistentDataType.LONG, now);
		}
		return true;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		// rcon is a RemoteConsoleCommandSender, not ConsoleCommandSender
		if(!(sender instanceof Player)) {
			if(args.length == 0) {
				return true;
			}
			switch (args[0]) {
			case "give":
				// /voucher give Reecepbcups <voucher> [amount]
				if(args.length < 3) {
					sender.sendMessage("&c/voucher give <player> <voucher> [amount]");
					return true;
				}
				int amount = 1;
				if(args.length >= 4 && Util.isInt(args[3])) {
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
				Bukkit.getOnlinePlayers().stream().forEach(t -> Schedulers.entity(plugin, t, () -> t.getInventory().addItem(item)));
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
		Schedulers.entity(plugin, target, () -> target.getInventory().addItem(createItem(Voucher, amount)));
	}


	@EventHandler(ignoreCancelled = true) // gives itemstack on click
	public void onInventoryClick(InventoryClickEvent event) {
		ItemStack clicked = event.getCurrentItem();

		// holder check instead of comparing titles, which builds a string on every click server wide
		if(event.getView().getTopInventory().getHolder(false) instanceof GuiHolder) {
			if(clicked == null) {
				return;
			}
			event.setCancelled(true);
			event.getWhoClicked().getInventory().addItem(clicked);
		}
	}


	private static final class GuiHolder implements InventoryHolder {
		@Override
		public Inventory getInventory() {
			return GUI;
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
