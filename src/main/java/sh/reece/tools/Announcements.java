package sh.reece.tools;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import sh.reece.utiltools.Util;

public class Announcements {

    public static void broadcast(boolean center, String line) {
        line = ConfigUtils.replaceVariable(line).trim();

        if (Main.isPAPIEnabled() && line.contains("%")) {
            var online = Bukkit.getOnlinePlayers();
            if (!online.isEmpty()) {
                line = PlaceholderAPI.setPlaceholders(online.iterator().next(), line);
            }
        }

        if (line.contains("<command=")) {
            String cmd = StringUtils.substringBetween(line, "<command=", "/>").trim();
            String actualMessage = Util.color(line.split("/>")[1]);
            if (actualMessage.contains("<center>")) {
                actualMessage = Util.centerMessage(actualMessage.replace("<center>", ""));
            }
            TextComponent message = new TextComponent(actualMessage);
            message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
            Bukkit.getServer().spigot().broadcast(message);
            return;
        }

        if (line.contains("<link=")) {
            String url = StringUtils.substringBetween(line, "<link=", "/>").trim();
            String actualMessage = Util.color(line.split("/>")[1]);
            if (actualMessage.contains("<center>")) {
                actualMessage = Util.centerMessage(actualMessage.replace("<center>", ""));
            }
            TextComponent message = new TextComponent(actualMessage);
            message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
            Bukkit.getServer().spigot().broadcast(message);
            return;
        }

        String manipulatedLine;
        if (line.contains("<center>") || center) {
            manipulatedLine = Util.centerMessage(line.replace("<center>", ""));
        } else {
            manipulatedLine = line;
        }
        Bukkit.broadcastMessage(Util.color(manipulatedLine));
    }
}
