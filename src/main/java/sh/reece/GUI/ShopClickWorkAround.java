package sh.reece.GUI;

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
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onInvClick(InventoryClickEvent e) {

		if (e.getCurrentItem() == null) {
			return;
		}

		String inv_name = Util.color(e.getView().getTitle());

		String InvName = ShopGUIMenuName;

		if (ShopGUIMenuName.equalsIgnoreCase(inv_name)) {

			if (!e.getCurrentItem().hasItemMeta()) {
				return;
			}

			String ITEMCLICKED = Util.color(e.getCurrentItem().getItemMeta().getDisplayName());
			Player p = (Player) e.getWhoClicked();

			if (plugin.getConfig().getString("ShopWorkAround.DEBUG").equalsIgnoreCase("true")) {
				p.sendMessage(" ");
				p.sendMessage("[ShopWorkaround] InvName: " + InvName);
				p.sendMessage("       -->   itemClicked: " + ITEMCLICKED);
			}

			ConfigurationSection Items = plugin.getConfig().getConfigurationSection("ShopWorkAround.RemappedClicks");

			for (String key : Items.getKeys(false)) {

				if (plugin.getConfig().getString("ShopWorkAround.DEBUG").equalsIgnoreCase("true")) {
					p.sendMessage("");
					p.sendMessage("[TOOLS:ShopPatch] ItemsToRemapClicks: " + Util.color(Items.getString(key + ".name")));
					p.sendMessage(" --> CMD: " + Util.color(Items.getString(key + ".command")));
				}

				if (ITEMCLICKED.equalsIgnoreCase(Util.color(Items.getString(key + ".name")))) {
					e.setCancelled(true);

					if (Items.getString(key + ".CloseInvBeforeCommand").equalsIgnoreCase("true")) {
						p.closeInventory();
					}

					String command = Items.getString(key + ".command");

					p.performCommand(command);
				}
			}
		}
	}
}
