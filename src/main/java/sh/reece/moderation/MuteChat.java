package sh.reece.moderation;

import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class MuteChat implements Listener, CommandExecutor {

	private static Main plugin;
	public Boolean ChatEnabled;
	public String MuteChatPerm, MuteChatBypassPerm, ENABLED, DISABLED;
	private ConfigUtils ConfigUtils;

	public MuteChat(Main instance) {
        plugin = instance;

		ChatEnabled = true;
    	
        if (plugin.enabledInConfig("Moderation.MuteChat.Enabled")) {
			ConfigUtils = plugin.getConfigUtils();
			
			MuteChatPerm = plugin.getConfig().getString("Moderation.MuteChat.Permission.Use");
			MuteChatBypassPerm = plugin.getConfig().getString("Moderation.MuteChat.Permission.Bypass");

			ENABLED = plugin.getConfig().getString("Moderation.MuteChat.Messages.Enabled");
			DISABLED = plugin.getConfig().getString("Moderation.MuteChat.Messages.Disable");
        	
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);	
			plugin.getCommand("mutechat").setExecutor(this);
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
		if(!ChatEnabled) {			
			if(!(event.getPlayer().hasPermission(MuteChatBypassPerm))) {
				event.getPlayer().sendMessage(ConfigUtils.lang("MUTECHAT_ISMUTED"));
				event.setCancelled(true);
			}	
		}
		
	}
	
	@Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("mutechat")) {
        	
        	if(!(sender.hasPermission(MuteChatPerm))) {
        		sender.sendMessage(Util.color("&cYou do not have permission to use this command!"));
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
