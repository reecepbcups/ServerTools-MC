package sh.reece.moderation;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class ClearChat extends BaseCommand {

	public Integer ClearChatLoops;

	public ClearChat(Main instance) {
	        super(instance, "Moderation.ClearChat", "clearchat");

	        if (isEnabled()) {
	 	        this.ClearChatLoops = instance.getConfig().getInt("Moderation.ClearChat.Messages.lines");
			}
	}

	@Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("clearchat")) {

        	if(noPermission(sender, cmd)) {
        		return true;
        	}

        	for(int i = 0; i < ClearChatLoops; ++i) {
        		Bukkit.broadcastMessage(" ");
        	}

        	String clearMsg = plugin.getConfig().getString("Moderation.ClearChat.Messages.msg", "&aChat cleared by %player%");
        	Bukkit.broadcastMessage(Util.color(clearMsg.replace("%player%", sender.getName())));


        }
        return true;
    }

}
