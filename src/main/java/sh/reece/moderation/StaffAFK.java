package sh.reece.moderation;

import java.io.File;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

public class StaffAFK extends BaseCommand implements Listener {

	//LuckPerms luckPerms = LuckPermsProvider.get();
	public String StaffAFKGroup;
	public Collection<String> staffRanks;
	public static String FILE_NAME;

	public FileConfiguration config, MAINCONFIG;

	// used for PAPI - read from the async PAPI thread, mutated on the main thread
	private static Set<UUID> staffWhoAreAFK = ConcurrentHashMap.newKeySet();
	public static boolean isStaffAfk(UUID uuid) {
		return staffWhoAreAFK.contains(uuid);
	}


	public StaffAFK(Main instance) {
	    super(instance, "Moderation.StaffAFK", "staffafk");

	    if (isEnabled()) {
	    	MAINCONFIG = instance.getConfig();
	    	this.StaffAFKGroup = MAINCONFIG.getString("Moderation.StaffAFK.StaffAFKGroup");
		    this.staffRanks = MAINCONFIG.getStringList("Moderation.StaffAFK.StaffGroups");

		    FILE_NAME = File.separator + "DATA" + File.separator + "StaffAFKDatabase.yml";
		    configUtils.createDirectory("DATA");
			configUtils.createFile(FILE_NAME);
			config = configUtils.getConfigFile(FILE_NAME);
		}
	}

	@EventHandler(ignoreCancelled = true)
  	public void StaffQuit(PlayerQuitEvent e) {
		removeStaffAFK(e.getPlayer());
	}

	private void removeStaffAFK(Player p) {
		UUID uuid = p.getUniqueId();
		if(staffWhoAreAFK.contains(uuid)) {
			if(config.getString(uuid.toString()) != null) {

				// Remove staffafk commands
				for(String removeCMD : MAINCONFIG.getStringList("Moderation.StaffAFK.RemoveAFK")) {
					removeCMD = removeCMD.replace("%player%", p.getName());
					removeCMD = removeCMD.replace("%PlayerConfigPrimarygroup%", config.getString(uuid.toString()+".primarygroup"));
					removeCMD = removeCMD.replace("%StaffAFKGroupName%", StaffAFKGroup);
					Util.console(removeCMD);
				}

				// reset op if they were before
				Boolean wasOpped = config.getBoolean(uuid.toString()+".isOp");
				if(wasOpped != null) {
					if(wasOpped) {
						p.setOp(true);
					}
				}

				config.set(uuid.toString(), null);
				configUtils.saveConfig(config, FILE_NAME);

				staffWhoAreAFK.remove(uuid);
			}
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("This command can only be used by players.");
			return true;
		}

		Player p = (Player) sender;
		UUID uuid = p.getUniqueId();

		if(noPermission(sender, cmd)) {
			return true;
		}

		User user = LuckPermsProvider.get().getUserManager().getUser(uuid);

		if(getPlayerGroup(p, staffRanks) == null && !isPlayerInGroup(p, StaffAFKGroup)) {
			p.sendMessage("You are not in the staff ranks group");
			return true;
		}

		// if user is not yet in the config file
		if(config.getString(uuid.toString()) == null) {

			config.set(uuid.toString()+".name", p.getName()); // only used for staff management behind the scenes
			config.set(uuid.toString()+".primarygroup", user.getPrimaryGroup().toString());
			config.set(uuid.toString()+".isOp", p.isOp());

			for(String giveCMD : MAINCONFIG.getStringList("Moderation.StaffAFK.GiveAFK")) {
				giveCMD = giveCMD.replace("%player%", p.getName());
				giveCMD = giveCMD.replace("%StaffAFKGroupName%", StaffAFKGroup);
				giveCMD = giveCMD.replace("%UsersPrimaryGroup%", user.getPrimaryGroup().toString());

				Util.console(giveCMD);
			}

			String output = "\n&eAdded you to StaffAFK\n";

			if(p.isOp()) { // remove op from them for now, reapply on unafk
				p.setOp(false);
				output += "&7&o(Removed you from OP as well)\n";
			}

			staffWhoAreAFK.add(p.getUniqueId());
			Util.coloredMessage(sender, output);

		} else {
			String primaryGroup = config.getString(p.getUniqueId()+".primarygroup");

			String output = "\n&eYou are back to your group: " + primaryGroup;

			for(String removeCMD : MAINCONFIG.getStringList("Moderation.StaffAFK.RemoveAFK")) {
				removeCMD = removeCMD.replace("%player%", p.getName());
				removeCMD = removeCMD.replace("%PlayerConfigPrimarygroup%", primaryGroup);
				removeCMD = removeCMD.replace("%StaffAFKGroupName%", StaffAFKGroup);
				Util.console(removeCMD);
			}

			if(config.getBoolean(p.getUniqueId().toString()+".isOp")) {
				p.setOp(true);
				output += "\n&a&o(Reapplied OP status)\n";
			}

			config.set(p.getUniqueId().toString(), null);
			staffWhoAreAFK.remove(p.getUniqueId());
			Util.coloredMessage(sender, output);
		}

		configUtils.saveConfig(config, FILE_NAME);
		return true;
	}

	public static boolean isPlayerInGroup(Player player, String group) {
	    return player.hasPermission("group." + group); //&& !(player.isOp());
	}

	public static String getPlayerGroup(Player player, Collection<String> possibleGroups) {
	    for (String group : possibleGroups) {
	        if (player.hasPermission("group." + group)) {
	            return group;
	        }
	    }
	    return null;
	}

}
