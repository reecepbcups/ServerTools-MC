package sh.reece.GUI;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;
import sh.reece.utiltools.Util;

public class ShopClickWorkAround extends ToggleableListener {

	private static String ShopGUIMenuName;
	private FileConfiguration Shop;
	private String shopPlugin;

	private static boolean DEBUG;
	// clicked item name (colored, lowercased) -> what to do
	private static Map<String, Remap> remaps = new HashMap<>();

	private static final class Remap {
		final String command;
		final boolean closeInv;
		Remap(String command, boolean closeInv) {
			this.command = command;
			this.closeInv = closeInv;
		}
	}

	public ShopClickWorkAround(Main instance) {
		super(instance, "ShopWorkAround");

		if (isEnabled()) {
			shopPlugin = instance.getConfig().getString("ShopWorkAround.plugin");
			try {
				Bukkit.getServer().getPluginManager().getPlugin(shopPlugin).getConfig();
			} catch (Exception e) {
				Util.consoleMSG("&e[ServerTools] &cShop plugin &n" + shopPlugin + "&c not found for \"ShopWorkAround\"");
				return;
			}

			Shop = Bukkit.getServer().getPluginManager().getPlugin(instance.getConfig().getString("ShopWorkAround.plugin")).getConfig();
			ShopGUIMenuName = Util.color(Shop.getString(instance.getConfig().getString("ShopWorkAround.MenuNameInConfig")));

			// cache everything the click handler needs so it never touches config per click
			DEBUG = "true".equalsIgnoreCase(instance.getConfig().getString("ShopWorkAround.DEBUG"));
			remaps = new HashMap<>();
			ConfigurationSection items = instance.getConfig().getConfigurationSection("ShopWorkAround.RemappedClicks");
			if (items != null) {
				for (String key : items.getKeys(false)) {
					String name = Util.color(items.getString(key + ".name"));
					String command = items.getString(key + ".command");
					boolean closeInv = "true".equalsIgnoreCase(items.getString(key + ".CloseInvBeforeCommand"));
					remaps.put(name.toLowerCase(), new Remap(command, closeInv));
				}
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onInvClick(InventoryClickEvent e) {

		if (e.getCurrentItem() == null) {
			return;
		}

		// cheap title guard first - bail before touching item meta on unrelated menus
		String inv_name = Util.color(e.getView().getTitle());
		if (!ShopGUIMenuName.equalsIgnoreCase(inv_name)) {
			return;
		}

		if (!e.getCurrentItem().hasItemMeta()) {
			return;
		}

		String ITEMCLICKED = Util.color(e.getCurrentItem().getItemMeta().getDisplayName());
		Player p = (Player) e.getWhoClicked();

		if (DEBUG) {
			p.sendMessage(" ");
			p.sendMessage("[ShopWorkaround] InvName: " + ShopGUIMenuName);
			p.sendMessage("       -->   itemClicked: " + ITEMCLICKED);
		}

		Remap remap = remaps.get(ITEMCLICKED.toLowerCase());
		if (remap != null) {
			e.setCancelled(true);

			if (remap.closeInv) {
				p.closeInventory();
			}

			p.performCommand(remap.command);
		}
	}
}
