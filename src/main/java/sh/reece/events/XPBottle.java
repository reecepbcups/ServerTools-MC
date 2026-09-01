package sh.reece.events;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class XPBottle extends BaseCommand implements Listener {

	public String bottlename = Util.color("&e&lEXP-Bottle &7(Right-Click)");
	public List<String> lore = new ArrayList<>();

	private ConfigUtils configUtils;

	public XPBottle(Main instance) {
		super(instance, "Misc.XPBottle", "xpbottle");
		configUtils = instance.getConfigUtils();

		if (isEnabled()) {
			lore.add(configUtils.lang("XPBOTTLE_BOTTLE_LORE1"));
			lore.add(configUtils.lang("XPBOTTLE_BOTTLE_LORE2"));
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onExpBottleEvent(PlayerInteractEvent e) {
		Player p = e.getPlayer();

		if (!(e.getAction().equals(Action.RIGHT_CLICK_BLOCK) || e.getAction().equals(Action.RIGHT_CLICK_AIR))) {
			return;
		}

		if (e.getItem() == null) {
			return;
		}

		ItemStack item = e.getItem();
		if (item.hasItemMeta()) {

			if (!item.getItemMeta().hasDisplayName()) {
				return;
			}
			if (!(item.getType() == Material.EXPERIENCE_BOTTLE)) {
				return;
			}

			if (item.getItemMeta().getDisplayName().equalsIgnoreCase(bottlename)) {
				if (!item.getItemMeta().hasLore() || item.getItemMeta().getLore().isEmpty()) {
					return;
				}
				String[] loreParts = ChatColor.stripColor(item.getItemMeta().getLore().get(0)).split(" ");
				if (loreParts.length < 2) {
					return;
				}

				int currentEXP = Util.getTotalExperience(p);
				int value = Integer.parseInt(loreParts[1]);
				int xpbottleAMT = item.getAmount();
				String finalOutput = configUtils.lang("XPBOTTLE_REDEEEMED").replace("%exp%", Util.formatNumber(value));

				e.setUseItemInHand(Result.DENY);

				if (p.isSneaking()) {
					p.getInventory().setItem(p.getInventory().getHeldItemSlot(), null);
					Util.setTotalExperience(p, currentEXP + (value * xpbottleAMT));
					Util.coloredMessage(p,
							finalOutput + "&r &f&lx" + xpbottleAMT + " &7&o(( " + Util.formatNumber(value * xpbottleAMT) + " EXP ))");
					return;
				}

				Util.removeItemFromPlayer(p, item, 1);
				Util.setTotalExperience(p, currentEXP + value);
				Util.coloredMessage(p, finalOutput);
			}
		}
	}

	// /xp command
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onXpCommand(PlayerCommandPreprocessEvent e) {
		if (!e.getMessage().toLowerCase().startsWith("/xp")) {
			return;
		}

		if ((e.getMessage().split(" ").length != 1) || e.getMessage().length() > 3) {
			return;
		}

		e.setCancelled(true);

		Player p = e.getPlayer();
		String EXP = "" + Util.getTotalExperience(p);

		p.sendMessage("");
		Util.coloredMessage(p, configUtils.lang("XPBOTTLE_XPCMD").replace("%exp%", EXP));
		p.sendMessage("");
	}

	// on withdraw
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender instanceof ConsoleCommandSender) {

			int Amount = Integer.parseInt(args[0]);
			Player p = Bukkit.getPlayer(args[1]);

			if (p == null) {
				sender.sendMessage("Player " + args[1] + " is not online");
				return true;
			}
			p.getInventory().addItem(new ItemStack[]{makeBottle(Amount, "CONSOLE")});

			return true;
		}

		Player p = (Player) sender;

		if (args.length > 0) {
			if (args.length == 1) {
				try {

					int XPWithdraw;
					if (args[0].equalsIgnoreCase("all")) {
						XPWithdraw = Util.getTotalExperience(p);
					} else {
						XPWithdraw = Integer.parseInt(args[0]);
					}

					int currentEXP = Util.getTotalExperience(p);

					if (XPWithdraw <= 0) {
						Util.coloredMessage(p, "&cEnter a number >0 EXP");
						return true;
					}

					if (!(currentEXP >= XPWithdraw)) {
						Util.coloredMessage(p, "&cYou do not have enough exp to withdraw " + XPWithdraw);
						return true;
					}

					Util.coloredMessage(p, configUtils.lang("XPBOTTLE_WITHDREW").replace("%xp%", XPWithdraw + ""));
					Util.setTotalExperience(p, currentEXP - XPWithdraw);
					p.getInventory().addItem(makeBottle(XPWithdraw, p.getName()));

				} catch (NumberFormatException e) {
					Util.coloredMessage(p, configUtils.lang("XPBOTTLE_ONLY_WHOLE"));
					return true;
				}
			}
		} else {
			Util.coloredMessage(p, "&d/xpbottle <amount / all>");
			Util.coloredMessage(p, "&7Convert <amount> xp into bottle form.");
			Util.coloredMessage(p, "&7Use &e/xp &7to view your current exp points.");
		}
		return true;
	}

	public ItemStack makeBottle(int Amount, String Signer) {

		List<String> updatedLore = new ArrayList<>();
		for (String s : lore) {
			s = s.replace("%value%", "" + Amount).replace("%player%", Signer);
			updatedLore.add(s);
		}

		ItemStack is = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName(bottlename);
		im.setLore(updatedLore);
		is.setItemMeta(im);
		return is;
	}
}
