package sh.reece.core;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
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

import sh.reece.tools.AlternateCommandHandler;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class InvSee implements CommandExecutor, Listener {// ,TabCompleter,Listener {

	private String Section, Permission, ModifyOthers, preventModify;
	private Set<UUID> openInvsee = new HashSet<>();
	private Main plugin;

	public InvSee(Main instance) {
		this.plugin = instance;

		Section = "Core.InvSee";

		// https://essinfo.xeya.me/permissions.html
		if (plugin.enabledInConfig(Section + ".Enabled")) {
			plugin.getCommand("invsee").setExecutor(this);
			Permission = plugin.getConfig().getString(Section + ".Permission");
			ModifyOthers = plugin.getConfig().getString(Section + ".ModifyOthers");
			preventModify = plugin.getConfig().getString(Section + ".StaffNoModify");

			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
		} else {
			AlternateCommandHandler.addDisableCommand("invsee");
		}

	}

	private boolean isInvsee(Player player) {
		if (openInvsee.contains(player.getUniqueId())) {
			return true;
		}
		return false;
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

		// can do /invsee
		if (!sender.hasPermission(Permission)) {
			sender.sendMessage(Util.color("&cYou do not have access to &n/" + label + "&c."));
			return true;
		}
		if (args.length < 1) {
			sender.sendMessage(Util.color("&cYou need to specify someone -> /invsee <player>"));
			return true;
		}

		Player target = Bukkit.getPlayer(args[0]);

		final Inventory inv;

		if (args.length > 1) {
			inv = Bukkit.getServer().createInventory(target, 9, args[0] + " Armour");
			inv.setContents(target.getInventory().getArmorContents());
			// if(!Util.isVersion1_8()){
			// inv.setItem(4, target.getInventory().getItemInHand());
			// }
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

		//Main.logging("Invsee run invclickevent - before CHEST type");

		// player view of invsee
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
        } else if (type == InventoryType.CHEST) { // amour view of Invsee
			final InventoryHolder invHolder = top.getHolder();
			//Main.logging("Event run with type chest for Invsee");

			if (invHolder instanceof HumanEntity && isInvsee(player) && event.getClick() != ClickType.MIDDLE) {
				//Main.logging("checking modify other");
				if(!player.hasPermission(ModifyOthers)){
					//Main.logging("canceled event correctly");
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
				// refreshPlayer = player;
			}

		}

	}

	public void closeAllViewedInvsee() {
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(openInvsee.contains(p.getUniqueId())){
				p.getOpenInventory().close();
			}
		}
		openInvsee.clear();
	}

}
