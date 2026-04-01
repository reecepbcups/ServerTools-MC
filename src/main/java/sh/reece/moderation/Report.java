package sh.reece.moderation;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class Report extends BaseCommand {

	private final HashMap<String, Long> CooldownHash = new HashMap<>();
	private Integer CooldownSeconds;
	private String perm, CooldownMSG, ReportSuccess;

	public Report(Main instance) {
        super(instance, "Moderation.Report", "report");

        if(isEnabled()) {
        	perm = "report.notify";

        	CooldownSeconds = instance.getConfig().getInt(section+".Cooldown", 15);
        	CooldownMSG = instance.getConfig().getString(section+".CooldownMSG");
        	ReportSuccess = instance.getConfig().getString(section+".ReportSuccess");
    	}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("This command can only be used by players.");
			return true;
		}

		Player p = (Player) sender;

		if (args.length <= 1) {
			sendHelpMenu(p);
			return true;
		}

		if(!Util.cooldown(CooldownHash, CooldownSeconds, p.getName(), CooldownMSG)) {
			// User has cooldown

    	} else {
    		// report player to online staff
    		Player target = Bukkit.getPlayer(args[0]);
    		if(target != null) {

    			if(args[0].equalsIgnoreCase(p.getName())) {
    				Util.coloredMessage(p, configUtils.lang("REPORT_SELF"));
    				return true;
    			}

    			ReportSuccess = ReportSuccess
    					.replace("%reporter%", p.getName())
    					.replace("%offender%", args[0])
    					.replace("%reason%", Util.argsToSingleString(1, args));

    			Bukkit.broadcast(Util.color(ReportSuccess), perm);
    			Util.coloredMessage(p, configUtils.lang("REPORT_SUCCESS").replace("%target%", args[0]));
    		} else {
    			Util.coloredMessage(p, configUtils.lang("REPORT_OFFLINE").replace("%target%", args[0]));
    		}

    	}
		return true;


	}

	public void sendHelpMenu(Player p) {
		Util.coloredMessage(p, "&f/report &7<player> <reason>");
	}



}
