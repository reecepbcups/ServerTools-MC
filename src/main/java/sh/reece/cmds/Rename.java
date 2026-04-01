package sh.reece.cmds;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class Rename extends BaseCommand implements Listener {

	private String lorePermisssion;
	private List<String> disabledRenameItems;

	public Rename(Main instance) {
		super(instance, "Misc.Rename", "rename");

		if(isEnabled()) {
			lorePermisssion = instance.getConfig().getString(section+".lorePermission");
			disabledRenameItems = instance.getConfig().getStringList(section+".disabledRename");
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void disabledRenamedItems(InventoryClickEvent event) {
		if (!(event.getInventory() instanceof AnvilInventory)) {
			return;
		}
		if (event.getSlotType() != SlotType.RESULT) {
			return;
		}
		if (event.getCurrentItem() == null) {
			return;
		}
		if (disabledRenameItems.contains(event.getCurrentItem().getType().toString())) {
			event.getWhoClicked().sendMessage(configUtils.lang("RENAME_DENYITEM")
					.replace("%item%", event.getCurrentItem().getType().toString()));
			event.setCancelled(true);
		}
	}

	private static final List<String> possibleArugments = new ArrayList<String>();
	private static final List<String> result = new ArrayList<String>();
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		if(possibleArugments.isEmpty()) {
			possibleArugments.add("name");
			possibleArugments.add("lore");

		}
		result.clear();
		if(args.length == 1) {
			for(String a : possibleArugments) {
				if(a.toLowerCase().startsWith(args[0].toLowerCase())) {
					result.add(a);
				}
			}
			return result;
		}
		return null;
	}


	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player p = (Player) sender;

		if (args.length == 0) {
			helpMenu(p);
			return false;
		}

		ItemStack item = getItem(p);
		if (item == null || item.getType() == Material.AIR) {
			Util.coloredMessage(p, "&cYou must be holding an item!");
			return true;
		}
		ItemMeta im = item.getItemMeta();
		if (im == null) {
			Util.coloredMessage(p, "&cThis item cannot be renamed.");
			return true;
		}

		String itemType = item.getType().toString().replace("LEGACY_", "");
		if(disabledRenameItems.contains(itemType)) {
			Util.coloredMessage(p, configUtils.lang("RENAME_DENYITEM").replace("%item%", itemType));
			return true;
		}

		int lineToChange;
		List<String> lore;

		switch(args[0]){

		case "name":

			if (!hasPermission(p, permission)) {
				Util.coloredMessage(p, "&cNo Permission to Rename item Names :(");
				return true;
			}

			String newName = Util.argsToSingleString(1, args);

			im.setDisplayName(Util.color(newName));
			item.setItemMeta(im);

			Util.coloredMessage(p, configUtils.lang("RENAME_SUCCESS"));
			break;

		case "lore":

			if (!hasPermission(p, lorePermisssion)) {
				Util.coloredMessage(p, "&cNo Permission to Rename item Lores :(");
				return true;
			}

			if(args.length <= 2) {
				helpMenu(p);
				return true;
			}

			switch (args[1]) {
			case "remove":
			case "delete":

				if(args.length < 3) {
					Util.coloredMessage(p, "&c/rename lore remove <LINE (0->48)>");
					return true;
				}
				lineToChange = Integer.valueOf(args[2]);
				//Util.consoleMSG(lineToChange+"");

				if(im.hasLore()) {
					int loreSize = im.getLore().size();

					if(loreSize <= lineToChange) {
						Util.coloredMessage(p, configUtils.lang("RENAME_NOT_ENOUGH_LORE")
								.replace("%line%", args[2]).replace("%size%", loreSize+""));
						return true;
					}
					lore = im.getLore();
					lore.remove(lineToChange);

					im.setLore(lore);
					item.setItemMeta(im);
				}


				break;

			case "add":
			case "append":

				if(args.length < 4) {
					Util.coloredMessage(p, "&c/rename lore add <line#> TextToAdd");
					return true;
				}

				lineToChange = Integer.valueOf(args[2]);
				if(lineToChange < 0) {
					Util.coloredMessage(p, "&cLore line must be >= 0");
					return true;
				}

				String newLineInLore = Util.argsToSingleString(3, args);

				// This is what we modify after getting old lore items
				if(im.hasLore()) {
					lore = im.getLore();
				} else {
					lore = new ArrayList<String>();
				}

				// adds blank lines if you wanted to change a later line
				if(lineToChange >= lore.size()) {
					int difference = lineToChange-lore.size();
					for(int i=0;i<=difference;i++) {
						lore.add(" ");
					}
				}

				lore.set(lineToChange, Util.color(newLineInLore));
				im.setLore(lore);
				item.setItemMeta(im);
				break;
			}
			return true;


		default:
			helpMenu(p);
			break;
		}

		return true;

	}

	private void helpMenu(Player p) {
		Util.coloredMessage(p, "&e&lRename Help Guide");
		Util.coloredMessage(p, "&f/rename name <name here>\n");
		Util.coloredMessage(p, "&f/rename lore remove <line#>");
		Util.coloredMessage(p, "&f/rename lore add <line#> <text>");
		Util.coloredMessage(p, "&7&o(( Where line number is 0 - 48 ))");
	}




	public ItemStack getItem(Player p) {
		return p.getInventory().getItemInMainHand();
	}


}
