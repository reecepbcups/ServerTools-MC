package sh.reece.moderation;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Freeze extends BaseCommand implements Listener {

	public Boolean ChatEnabled;
	public Set<UUID> frozenPlayerList;
	private final List<String> Messages;

	public Freeze(Main instance) {
        super(instance, "Moderation.Freeze", "freeze");

		frozenPlayerList = new HashSet<>();
		Messages = instance.getConfig().getStringList("Moderation.Freeze.Message");
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(!(cmd.getName().equalsIgnoreCase("freeze") || cmd.getName().equalsIgnoreCase("ss"))) {
			return true;
		}

		if(noPermission(sender, cmd)) {
  			return true;
		}

		if(args.length != 1) {
			Util.coloredMessage((Player)sender, "&f[!] &c/freeze <player> &7| &c/unfreeze <player>");
  			return true;
		}

		// try online first (free), only fall back to offline lookup
		Player onlineTarget = Bukkit.getPlayer(args[0]);

		if(onlineTarget != null) {
			UUID uid = onlineTarget.getUniqueId();
			if(frozenPlayerList.contains(uid)) {
				frozenPlayerList.remove(uid);
				Util.coloredMessage((Player)sender, "&a&n"+args[0]+"&a unfrozen!");
				Util.coloredMessage(onlineTarget, configUtils.lang("FREEZE_UNFROZEN"));
			} else {
				frozenPlayerList.add(uid);
				Util.coloredMessage((Player)sender, "&c&n"+args[0]+"&c has been frozen!");
				if(Messages != null && !Messages.isEmpty()) {
					Messages.forEach(msg -> Util.coloredMessage(onlineTarget, ConfigUtils.replaceVariable(msg)));
				}
			}
		} else {
			// offline: only check cache, never block main thread
			@SuppressWarnings("deprecation")
			OfflinePlayer offP = Bukkit.getOfflinePlayer(args[0]);
			if(offP == null || !offP.hasPlayedBefore()) {
				Util.coloredMessage((Player)sender, "&cError! Player not found");
				return true;
			}
			UUID uid = offP.getUniqueId();
			if(frozenPlayerList.contains(uid)) {
				frozenPlayerList.remove(uid);
				Util.coloredMessage((Player)sender, "&a&n"+args[0]+"&a unfrozen!");
			} else {
				frozenPlayerList.add(uid);
				Util.coloredMessage((Player)sender, "&c&n"+args[0]+"&c has been frozen!");
			}
		}
		return true;
	}



	@EventHandler(ignoreCancelled = true)
  	public void onMove(PlayerMoveEvent e) {
  		if(frozenPlayerList.isEmpty()) return;
  		if(e.getTo() == null) return;
  		if(e.getFrom().getBlockX() == e.getTo().getBlockX()
  			&& e.getFrom().getBlockY() == e.getTo().getBlockY()
  			&& e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;
  		if(frozenPlayerList.contains(e.getPlayer().getUniqueId())) {
  			e.setTo(e.getFrom());
  		}
  	}

	@EventHandler(ignoreCancelled = true)
  	public void onDrop(PlayerDropItemEvent e) {
  		if(frozenPlayerList.isEmpty()) return;
  		if(frozenPlayerList.contains(e.getPlayer().getUniqueId())) {
  			e.setCancelled(true);
  		}
  	}
	@EventHandler(ignoreCancelled = true)
  	public void onDrop(PlayerPickupItemEvent e) {
  		if(frozenPlayerList.isEmpty()) return;
  		if(frozenPlayerList.contains(e.getPlayer().getUniqueId())) {
  			e.setCancelled(true);
  		}
  	}

	@EventHandler(ignoreCancelled = true)
  	public void Damage(EntityDamageEvent e) {
		if(frozenPlayerList.isEmpty()) return;
		if (e.getEntity() instanceof Player){
			if(frozenPlayerList.contains(e.getEntity().getUniqueId())) {
	  			e.setCancelled(true);
	  		}
		}
  	}

	@EventHandler(ignoreCancelled = true)
  	public void announceLogout(PlayerQuitEvent e) {
		if(frozenPlayerList.contains(e.getPlayer().getUniqueId())) {
	  		Bukkit.broadcast(e.getPlayer().getName() + " logged out while frozen!", permission);
	  	}
	}
	@EventHandler(ignoreCancelled = true)
  	public void announceReLogin(PlayerJoinEvent e) {
		if(frozenPlayerList.contains(e.getPlayer().getUniqueId())) {
	  		Bukkit.broadcast(e.getPlayer().getName() + " has logged back in while frozen!", permission);
	  	}
	}

	@EventHandler(ignoreCancelled = true)
  	public void playerChat(AsyncPlayerChatEvent e) {
		if(frozenPlayerList.isEmpty()) return;
		if(frozenPlayerList.contains(e.getPlayer().getUniqueId())) {
	  		e.getPlayer().sendMessage(configUtils.lang("FREEZE_DENYCHAT"));
	  		e.setCancelled(true);
	  	}
	}


	@EventHandler(ignoreCancelled = true)
	public void onTeleport(PlayerTeleportEvent e) {
		if(frozenPlayerList.isEmpty()) return;
		if(frozenPlayerList.contains(e.getPlayer().getUniqueId())) {
			e.getPlayer().sendMessage(configUtils.lang("FREEZE_DENYTP"));
			e.setCancelled(true);
		}
	}


}
