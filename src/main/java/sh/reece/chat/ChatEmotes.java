package sh.reece.chat;


import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class ChatEmotes implements Listener {

	private static Main plugin;
	private ConfigurationSection msgCfg;
	private String permission;
	private HashMap<String, String> emojiDict;

	public ChatEmotes(Main instance) {
        plugin = instance;

        if(plugin.enabledInConfig("Chat.ChatEmoji.Enabled")) {

        	emojiDict = new HashMap<String, String>();
        	permission = plugin.getConfig().getString("Chat.ChatEmoji.permission");

    		// add all keys to memory on Enable
    		msgCfg = plugin.getConfig().getConfigurationSection("Chat.ChatEmoji.Emojis");
    		for(String key : msgCfg.getKeys(false)) {
    			emojiDict.put(key, msgCfg.getString(key));
    			//Util.consoleMSG(key + " -> " + msgCfg.getString(key));
    		}

    		Bukkit.getServer().getPluginManager().registerEvents(this, plugin);

    	}
	}


	@EventHandler(ignoreCancelled = true)
	public void onChat(AsyncPlayerChatEvent event) {
		String msg = event.getMessage();

		if(permission.length()!=0 || !event.getPlayer().hasPermission(permission)) {
			return;
		}

		for (String key : emojiDict.keySet()) {
			//Util.consoleMSG(key);

		    if (msg.contains(key)) {
		        msg = msg.replace(key, emojiDict.get(key));
		    }
		}
		event.setMessage(Util.color(msg));

	}
}
