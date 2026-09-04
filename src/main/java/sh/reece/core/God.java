package sh.reece.core;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;

public class God extends BaseCommand implements Listener {

	public static final String GOD_ENABLED = "<white>[!] God mode <green>enabled<white>.";
	public static final String GOD_DISABLED = "<white>[!] God mode <red>disabled<white>.";

	public God(Main instance) {
		super(instance, "Core.God", "god");
	}

	private static Set<UUID> GODS = new HashSet<>();

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (noPermission(sender, cmd)) return true;

		Player p = (Player) sender;
		UUID uid = p.getUniqueId();

		if (!GODS.remove(uid)) {
			GODS.add(uid);
			p.sendRichMessage(GOD_ENABLED);
		} else {
			p.sendRichMessage(GOD_DISABLED);
		}
		return true;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onDamage(EntityDamageEvent e) {
		if (GODS.isEmpty()) return;
		if (e.getEntity() instanceof Player p && GODS.contains(p.getUniqueId())) {
			e.setCancelled(true);
			p.setHealth(p.getMaxHealth());
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onQuit(PlayerQuitEvent e) {
		GODS.remove(e.getPlayer().getUniqueId());
	}
}
