package sh.reece.GUI;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.tools.Unloadable;
import sh.reece.utiltools.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class ChatColor extends BaseCommand implements Listener, Unloadable {

	public String perm;
	public static String InvName;
	public static String FILENAME;
	public static Inventory ColorINV;
	public static FileConfiguration config;

	public Random rand = new Random();
	public static Boolean isEnabled;

	// read from the async chat thread while the main thread writes
	public static Map<String, String> ChatColorHash = new java.util.concurrent.ConcurrentHashMap<String, String>();

	public static List<String> EMPTY_LORE = new ArrayList<String>();
	public static List<String> RainbowColors;

	private ConfigUtils configUtils;

	// key -> config entry, insertion order = GUI order
	private static Map<String, ConfigurationSection> Colors = new LinkedHashMap<String, ConfigurationSection>();
	private static Map<Integer, String> SlotKeys = new HashMap<Integer, String>();

	private boolean dataLoaded = false;

	public ChatColor(Main instance) {
		super(instance, "Chat.ChatColor", "chatcolor");

		isEnabled = false;
		configUtils = instance.getConfigUtils();

		if (isEnabled()) {
			isEnabled = true;
			FILENAME = File.separator + "DATA" + File.separator + "ChatColor.yml";
			perm = "Chatcolor.";
			InvName = configUtils.lang("CHATCOLOR_GUI");
			RainbowColors = instance.getConfig().getStringList("Chat.ChatColor.RainbowColors");

			// rebuild GUI on reload since Colors come from config
			ColorINV = null;
			Colors.clear();
			ConfigurationSection section = instance.getConfig().getConfigurationSection("Chat.ChatColor.Colors");
			if (section != null) {
				for (String key : section.getKeys(false)) {
					Colors.put(key, section.getConfigurationSection(key));
				}
			}
		}
	}

	private void ensureInvCreated() {
		if (ColorINV == null) {
			initCreateInv();
		}
	}

	private void ensureDataLoaded() {
		if (!dataLoaded) {
			configUtils.createDirectory("DATA");
			configUtils.createFile(FILENAME);
			config = configUtils.getConfigFile(FILENAME);
			loadToMemory();
			dataLoaded = true;
		}
	}

	@Override
	public void onUnload() {
		saveChatColorToFile();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		ensureInvCreated();
		ensureDataLoaded();
		Player p = (Player) sender;
		p.openInventory(ColorINV);
		return true;
	}

	public void initCreateInv() {
		int rows = Math.max(1, Math.min(6, (Colors.size() + 8) / 9));
		ColorINV = Bukkit.createInventory(new GuiHolder(), rows * 9, InvName);
		SlotKeys.clear();

		int slot = 0;
		for (Map.Entry<String, ConfigurationSection> entry : Colors.entrySet()) {
			if (slot >= rows * 9) {
				break;
			}
			String key = entry.getKey();
			ConfigurationSection c = entry.getValue();

			// no inline defaults: getString(path, def) skips the jar defaults, which are all
			// that's loaded on the first boot after an upgrade
			String materialName = c.getString("material");
			Material material = materialName == null ? null : Material.matchMaterial(materialName);
			if (material == null) {
				material = Material.WHITE_STAINED_GLASS_PANE;
			}

			List<String> lore = new ArrayList<String>();
			lore.add("");
			lore.add("&f" + configUtils.lang("CHATCOLOR_INFO"));
			lore.add("&f" + configUtils.lang("CHATCOLOR_COLOR") + formatMessage(key, key));
			lore.add("&f" + configUtils.lang("CHATCOLOR_ACCESS"));
			lore.add("");
			lore.add(configUtils.lang("CHATCOLOR_SELECT"));

			createDisplay(ColorINV, new ItemStack(material, 1), slot, Objects.requireNonNullElse(c.getString("name"), key), lore);
			SlotKeys.put(slot, key);
			slot++;
		}
	}

	public static String getColor(String uuid) {
		if (ChatColorHash.containsKey(uuid)) {
			return ChatColorHash.get(uuid);
		}
		return null;
	}

	public void setColor(Player p, String key) {
		ChatColorHash.put(p.getUniqueId().toString(), key);
		Util.coloredMessage(p, configUtils.lang("CHATCOLOR_SET").replace("%color%", formatMessage(key, key)));
	}

	// stored is a Colors key, or a legacy raw code like "&c" from older data files
	public String formatMessage(String stored, String msg) {
		ConfigurationSection c = Colors.get(stored);
		if (c == null) {
			return stored.startsWith("&") ? stored + msg : msg;
		}
		String format = Objects.requireNonNullElse(c.getString("format"), "%message%");
		if (format.equalsIgnoreCase("rainbow")) {
			return rainbowFormat(msg);
		}
		return format.replace("%message%", msg);
	}

	public String rainbowFormat(String msg) {
		StringBuilder sb = new StringBuilder(msg.length() * 4);
		for (int i = 0; i < msg.length(); i++) {
			char c = msg.charAt(i);
			if (c != ' ') {
				sb.append(RainbowColors.get(rand.nextInt(RainbowColors.size()))).append(c);
			} else {
				sb.append(' ');
			}
		}
		return sb.toString();
	}

	public void loadToMemory() {
		if (config.getConfigurationSection("Data") != null) {
			for (String uuid : config.getConfigurationSection("Data").getKeys(false)) {
				ChatColorHash.put(uuid, config.getString("Data." + uuid));
			}
		}
	}

	public void saveChatColorToFile() {
		if (isEnabled) {
			if (ChatColorHash.keySet().size() > 0) {
				for (String uuid : ChatColorHash.keySet()) {
					config.set("Data." + uuid, ChatColorHash.get(uuid));
				}
				configUtils.saveConfig(config, FILENAME);
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void playerColoredChatEvent(AsyncPlayerChatEvent e) {
		ensureDataLoaded();
		String color = getColor(e.getPlayer().getUniqueId().toString());

		if (color != null) {
			e.setMessage(Util.color(formatMessage(color, e.getMessage())));
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent event) {
		// holder check instead of comparing titles, which builds a string on every click server wide
		if (event.getView().getTopInventory().getHolder(false) instanceof GuiHolder) {

			ItemStack clicked = event.getCurrentItem();
			if (clicked == null || !clicked.hasItemMeta()) {
				return;
			}

			Player p = (Player) event.getWhoClicked();
			String key = SlotKeys.get(event.getRawSlot());
			if (key == null) {
				event.setCancelled(true);
				return;
			}

			// "Chatcolor.[!] Red" is what older versions checked (stripped display name)
			String chatpermission = perm + key;
			if (p.hasPermission(chatpermission) || p.hasPermission(perm + "[!] " + key)) {
				setColor(p, key);
			} else {
				p.sendMessage(Util.color("&4&l[!]&c You do not have permission to use &N" + chatpermission));
			}

			p.closeInventory();
			event.setCancelled(true);
		}
	}

	private static final class GuiHolder implements InventoryHolder {
		@Override
		public Inventory getInventory() {
			return ColorINV;
		}
	}

	public static void createDisplay(Inventory inv, ItemStack itemStack, int Slot, String name, List<String> list) {

		ItemStack item = itemStack;
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(Util.color(name));

		ArrayList<String> Lore = new ArrayList<String>();
		if (list.size() > 0) {
			for (String l : list) {
				Lore.add(Util.color(l));
			}
		}
		meta.setLore(Lore);

		item.setItemMeta(meta);

		inv.setItem(Slot, item);
	}
}
