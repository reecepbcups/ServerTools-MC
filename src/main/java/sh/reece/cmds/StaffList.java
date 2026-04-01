package sh.reece.cmds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffectType;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import sh.reece.utiltools.Util;

public class StaffList extends BaseCommand {

	private Set<String> groups;

	// owner: "&8&l<&d&lOWNER&8&l> &f» &d"
	private HashMap<String, String> groupFormating = new HashMap<String, String>();

	public StaffList(Main instance) {
        super(instance, "Commands.StaffList", "stafflist");

        if (isEnabled()) {
        	if(configUtils.getConfigFile("config.yml").contains(section+".groups")) {
        		groups = configUtils.getConfigFile("config.yml").getConfigurationSection(section+".groups").getKeys(false);
        	} else {
        		Util.consoleMSG("&c[!] &4NO GROUPS DEFINED AT " + section+".groups");
        		return;
        	}

        	for(String group : groups) {
        		groupFormating.put(group, plugin.getConfig().getString(section+".groups."+group));
        	}
        }
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if(sender instanceof ConsoleCommandSender) {
			Util.consoleMSG("You can not run this command, try 'online' instead");
			return true;
		}

		Player p = (Player) sender;

		// owner, ['reece', 'phasha']
		HashMap<String, Set<String>> staff = new HashMap<String, Set<String>>();

		for(Player online : Bukkit.getOnlinePlayers()) {
			User u = LuckPermsProvider.get().getUserManager().getUser(online.getUniqueId());
			if(u == null) continue;
			String mainGroup = u.getPrimaryGroup().toString();

			if(!groups.contains(mainGroup)) {
				continue;
			}

			if(isVanished(online) || online.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
				continue;
			}

			if(!staff.containsKey(mainGroup)) {
				Set<String> newgroup = new HashSet<String>();
				newgroup.add(online.getName());
				staff.put(mainGroup, newgroup);

				Util.consoleMSG("Added " + mainGroup + " key to staff hash");
			} else {
				staff.get(mainGroup).add(online.getName());
			}
		}

		Util.coloredMessage(p, " ");
		for(String group : groups) {
			String finalOutput = "";

			String title = groupFormating.get(group);

			finalOutput+=title;

			if(!staff.containsKey(group)) {
				finalOutput+=" &c&oN/A";
			} else {
				for(String SPlayer : staff.get(group)) {
					finalOutput += SPlayer+" ";
				}
			}
			Util.coloredMessage(p, finalOutput);
		}
		Util.coloredMessage(p, " ");

		return true;
	}

	private boolean isVanished(Player player) {
		for (MetadataValue meta : player.getMetadata("vanished")) {
			if (meta.asBoolean()) {
				return true;
			}
		}
		return false;
	}
}
