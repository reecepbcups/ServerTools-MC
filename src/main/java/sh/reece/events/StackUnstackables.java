package sh.reece.events;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import sh.reece.tools.Main;

public class StackUnstackables implements Listener {

	private Main plugin;
	private Set<Material> mats = new HashSet<>();
	public StackUnstackables(Main instance) {
		plugin = instance;

		if (plugin.enabledInConfig("Events.StackUnstackables.Enabled")) {

			for(String _mat : plugin.getConfig().getStringList("Events.StackUnstackables.items")) {
				mats.add(Material.getMaterial(_mat.toUpperCase()));
			}

			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onItemPickUp(PlayerPickupItemEvent e) {
		Material pickedUpItem = e.getItem().getItemStack().getType();
		if (!mats.contains(pickedUpItem)) return;

		Player p = e.getPlayer();
		int pickedUpItemAmount = e.getItem().getItemStack().getAmount();

		for (int slot = 0; slot < p.getInventory().getSize(); slot++) {
			ItemStack invItem = p.getInventory().getItem(slot);
			if (invItem == null) continue;
			if (invItem.getType() != pickedUpItem) continue;

			int inv_amount = invItem.getAmount();
			if (inv_amount >= 64) continue;

			p.getInventory().setItem(slot, new ItemStack(pickedUpItem, inv_amount + pickedUpItemAmount));
			e.setCancelled(true);
			e.getItem().remove();
			return;
		}
	}
}
