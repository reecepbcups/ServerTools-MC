package sh.reece.chat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;
import sh.reece.utiltools.Util;

public class ChatEmotes extends ToggleableListener {

	private String permission;
	// parallel arrays avoid entrySet iterator allocation on every chat message
	private String[] emojiKeys;
	private String[] emojiValues;

	public ChatEmotes(Main instance) {
		super(instance, "Chat.ChatEmoji");

		if (isEnabled()) {
			permission = instance.getConfig().getString("Chat.ChatEmoji.permission");

			ConfigurationSection msgCfg = instance.getConfig().getConfigurationSection("Chat.ChatEmoji.Emojis");
			Map<String, String> dict = new LinkedHashMap<>();
			for (String key : msgCfg.getKeys(false)) {
				dict.put(key, msgCfg.getString(key));
			}
			emojiKeys = dict.keySet().toArray(new String[0]);
			emojiValues = dict.values().toArray(new String[0]);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onChat(AsyncPlayerChatEvent event) {
		String msg = event.getMessage();

		if (!permission.isEmpty() && !hasPermission(event.getPlayer())) {
			return;
		}

		for (int i = 0; i < emojiKeys.length; i++) {
			if (msg.contains(emojiKeys[i])) {
				msg = msg.replace(emojiKeys[i], emojiValues[i]);
			}
		}
		event.setMessage(Util.color(msg));
	}
}
