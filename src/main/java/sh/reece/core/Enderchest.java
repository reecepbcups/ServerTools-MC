package sh.reece.core;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.tools.Unloadable;
import sh.reece.utiltools.Util;

public class Enderchest extends BaseCommand implements Listener, Unloadable {

	private String ViewOthers, ModifyOthers;
	private Set<UUID> openEnderChest = new HashSet<>();

	public Enderchest(Main instance) {
		super(instance, "Core.Enderchest", "enderchest");
		if (isEnabled()) {
			ViewOthers = plugin.getConfig().getString(section + ".ViewOthers");
			ModifyOthers = plugin.getConfig().getString(section + ".ModifyOthers");
		}
	}

	private boolean isEnderSee(Player player) {
		return openEnderChest.contains(player.getUniqueId());
	}
	private void setEnderSee(Player player, boolean value) {
		UUID uuid = player.getUniqueId();
		if (value) {
			openEnderChest.add(uuid);
		} else {
			openEnderChest.remove(uuid);
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player target = (Player) sender;

		if (noPermission(sender, cmd)) return true;

		if (args.length >= 1) {
			if (sender.hasPermission(ViewOthers)) {
				target = Bukkit.getPlayer(args[0]);
			} else {
				Util.coloredMessage(sender, "&f[!] &cYou can not view &f" + args[0] + "'s&c enderchest");
			}
		}

		Player opener = (Player) sender;
		opener.closeInventory();
		setEnderSee(opener, !(target.equals(opener)));
		opener.openInventory(target.getEnderChest());
		return true;
	}

	@EventHandler(ignoreCancelled = true)
	public void onInventoryClickEvent(final InventoryClickEvent event) {
		final Inventory top = event.getView().getTopInventory();
		final InventoryType type = top.getType();

		if (type == InventoryType.ENDER_CHEST) {
			Player p = (Player) event.getWhoClicked();
			if (isEnderSee(p) && !(p.hasPermission(ModifyOthers))) {
				event.setCancelled(true);
				Util.coloredMessage(p, "&f[!] &cYou can not edit their enderchest&f!");
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onInvClose(final InventoryCloseEvent e) {
		Player refreshPlayer = null;
		final Inventory top = e.getView().getTopInventory();
		final InventoryType type = top.getType();

		if (type == InventoryType.ENDER_CHEST) {
			Player p = ((Player) e.getPlayer());
			setEnderSee(p, false);
			refreshPlayer = p;
		}
		if (refreshPlayer != null) {
			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, refreshPlayer::updateInventory, 1);
		}
	}

	@Override
	public void onUnload() {
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (openEnderChest.contains(p.getUniqueId())) {
				p.getOpenInventory().close();
			}
		}
		openEnderChest.clear();
	}

}
