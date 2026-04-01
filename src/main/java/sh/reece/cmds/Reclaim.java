package sh.reece.cmds;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;
import java.util.Set;

public class Reclaim extends BaseCommand {

	private FileConfiguration reclaimcnfg;
	private String FILENAME;
	private Set<String> RECLAIM_PERMISSIONS;
	private List<String> usedMemberReclaims;
	private int srtIDXforUsrOut;

	public Reclaim(Main instance) {
		super(instance, "Commands.reclaim", "reclaim");

		if (isEnabled()) {
			configUtils.createDirectory("DATA");
			FILENAME = File.separator + "DATA" + File.separator + "Reclaim.yml";
			configUtils.createFile(FILENAME);
			reclaimcnfg = configUtils.getConfigFile(FILENAME);

			RECLAIM_PERMISSIONS = plugin.getConfig().getConfigurationSection(section+".permissions").getKeys(false);
			srtIDXforUsrOut = plugin.getConfig().getInt(section+".BeginAtIndex");
		}
	}

	private String getPlayersGroupIfAny(Player p) {
		for(String perm : RECLAIM_PERMISSIONS) {
			if(p.hasPermission(perm.replace("_", ""))) {
				Util.consoleMSG(p.getPlayer().getName()+" has perm "+perm);
				return perm;
			}
		}
		return null;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		Player p = (Player) sender;

		if(p.isOp()) { Util.coloredMessage(p, "&4&l[!] &cRemeber you are OPPED"); }

		if(reclaimcnfg.getStringList("USED").contains(p.getUniqueId().toString())) {
			Util.coloredMessage(p, configUtils.lang("RECLAIM_ALREADY_CLAIMED"));
			return true;
		}

		String permission = getPlayersGroupIfAny(p);
		if(permission == null) {
			Util.coloredMessage(p, configUtils.lang("RECLAIM_NOTHING"));
			return true;
		}

		runCMDS(p, permission);
		return true;

	}

	private void runCMDS(Player p, String permission) {
		for(String command : plugin.getConfig().getStringList(section+".permissions."+permission)) {
			Util.console(command.replace("%player%", p.getName()));
		}
		Util.coloredMessage(p, configUtils.lang("RECLAIM_RECLAIMED")
				.replace("%permission%", permission.substring(srtIDXforUsrOut).toUpperCase()));

		usedMemberReclaims = reclaimcnfg.getStringList("USED");
		usedMemberReclaims.add(p.getUniqueId().toString());
		reclaimcnfg.set("USED", usedMemberReclaims);
		configUtils.saveConfig(reclaimcnfg, FILENAME);
	}

}
