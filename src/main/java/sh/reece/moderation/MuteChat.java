package sh.reece.moderation;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class MuteChat extends BaseCommand implements Listener {

	public Boolean ChatEnabled;
	public String MuteChatBypassPerm, ENABLED, DISABLED;

	public MuteChat(Main instance) {
        super(instance, "Moderation.MuteChat", "mutechat");

		ChatEnabled = true;

        if (isEnabled()) {
			MuteChatBypassPerm = instance.getConfig().getString("Moderation.MuteChat.Permission.Bypass", "mutechat.bypass");

			ENABLED = instance.getConfig().getString("Moderation.MuteChat.Messages.Enabled");
			DISABLED = instance.getConfig().getString("Moderation.MuteChat.Messages.Disable");
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
		if(!ChatEnabled) {
			if(!(event.getPlayer().hasPermission(MuteChatBypassPerm))) {
				event.getPlayer().sendMessage(configUtils.lang("MUTECHAT_ISMUTED"));
				event.setCancelled(true);
			}
		}

	}

	@Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("mutechat")) {

        	if(noPermission(sender, cmd)) {
        		return true;
        	}

            ChatEnabled = !ChatEnabled;

            String option;

            if(ChatEnabled) {
            	option = ENABLED;
            } else {
            	option = DISABLED;
            }

    		for (Player all : Bukkit.getOnlinePlayers()) {
				all.sendMessage(Util.color(option.replace("%player%", sender.getName())));
			}

        }
        return true;
    }



}
