package sh.reece.chat;

import java.util.HashMap;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;
import sh.reece.utiltools.Util;

public class ChatEmotes extends ToggleableListener {

	private ConfigurationSection msgCfg;
	private String permission;
	private HashMap<String, String> emojiDict;

	public ChatEmotes(Main instance) {
		super(instance, "Chat.ChatEmoji");

		if (isEnabled()) {
			emojiDict = new HashMap<String, String>();
			permission = instance.getConfig().getString("Chat.ChatEmoji.permission");

			msgCfg = instance.getConfig().getConfigurationSection("Chat.ChatEmoji.Emojis");
			for (String key : msgCfg.getKeys(false)) {
				emojiDict.put(key, msgCfg.getString(key));
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onChat(AsyncPlayerChatEvent event) {
		String msg = event.getMessage();

		if (permission.length() != 0 && !event.getPlayer().hasPermission(permission)) {
			return;
		}

		for (String key : emojiDict.keySet()) {
			if (msg.contains(key)) {
				msg = msg.replace(key, emojiDict.get(key));
			}
		}
		event.setMessage(Util.color(msg));
	}
}
