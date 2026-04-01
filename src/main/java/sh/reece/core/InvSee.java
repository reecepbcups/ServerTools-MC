package sh.reece.core;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.tools.Unloadable;
import sh.reece.utiltools.Util;

public class InvSee extends BaseCommand implements Listener, Unloadable {

	private String ModifyOthers, preventModify;
	private Set<UUID> openInvsee = new HashSet<>();

	public InvSee(Main instance) {
		super(instance, "Core.InvSee", "invsee");
		if (isEnabled()) {
			ModifyOthers = plugin.getConfig().getString(section + ".ModifyOthers");
			preventModify = plugin.getConfig().getString(section + ".StaffNoModify");
		}
	}

	private boolean isInvsee(Player player) {
		return openInvsee.contains(player.getUniqueId());
	}

	private void setInvSee(Player player, boolean value) {
		UUID uuid = player.getUniqueId();
		if (value) {
			openInvsee.add(uuid);
		} else {
			openInvsee.remove(uuid);
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (noPermission(sender, cmd)) return true;

		if (args.length < 1) {
			sender.sendMessage(Util.color("&cYou need to specify someone -> /invsee <player>"));
			return true;
		}

		Player target = Bukkit.getPlayer(args[0]);
		if (target == null) {
			sender.sendMessage(Util.color("&cPlayer " + args[0] + " is not online."));
			return true;
		}

		final Inventory inv;

		if (args.length > 1) {
			inv = Bukkit.getServer().createInventory(target, 9, args[0] + " Armour");
			inv.setContents(target.getInventory().getArmorContents());
		} else {
			inv = target.getInventory();
		}

		Player opener = (Player) sender;
		opener.closeInventory();
		opener.openInventory(inv);
		setInvSee(opener, !target.equals(opener));
		return true;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onInventoryClickEvent(final InventoryClickEvent event) {
		Player refreshPlayer = null;
		final Inventory top = event.getView().getTopInventory();
		final InventoryType type = top.getType();
		final Player player = (Player) event.getWhoClicked();

		if (type == InventoryType.PLAYER) {
			final InventoryHolder invHolder = top.getHolder();
			if (invHolder instanceof HumanEntity) {
				final Player invOwner = (Player) invHolder;

				if (isInvsee(player)
						&& (!player.hasPermission(ModifyOthers)
						|| invOwner.hasPermission(preventModify)
						|| !invOwner.isOnline())) {

					event.setCancelled(true);
					refreshPlayer = player;
				}
			}
		} else if (type == InventoryType.CHEST) {
			final InventoryHolder invHolder = top.getHolder();

			if (invHolder instanceof HumanEntity && isInvsee(player) && event.getClick() != ClickType.MIDDLE) {
				if (!player.hasPermission(ModifyOthers)) {
					event.setCancelled(true);
				}
				refreshPlayer = player;
			}
		}
		if (refreshPlayer != null) {
			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, refreshPlayer::updateInventory, 1);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onInvClose(final InventoryCloseEvent e) {
		final Inventory top = e.getView().getTopInventory();
		final InventoryType type = top.getType();
		final Player player = (Player) e.getPlayer();

		if (type == InventoryType.CHEST && top.getSize() == 9) {
			if (top.getHolder() instanceof HumanEntity) {
				setInvSee(player, false);
			}
		}
	}

	@Override
	public void onUnload() {
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (openInvsee.contains(p.getUniqueId())) {
				p.getOpenInventory().close();
			}
		}
		openInvsee.clear();
	}

}
