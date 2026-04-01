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

	@SuppressWarnings("deprecation")
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		// not needed, registering cmd does it dummy
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


		OfflinePlayer p = Bukkit.getOfflinePlayer(args[0]);

		if(p == null || !p.hasPlayedBefore()) {
			Util.coloredMessage((Player)sender, "&cError! Player not found");
	  		return true;
		}

		if(frozenPlayerList.contains(p.getUniqueId())) {
			frozenPlayerList.remove(p.getUniqueId());
	  		Util.coloredMessage((Player)sender, "&a&n"+args[0]+"&a unfrozen!");

	  		if(Bukkit.getServer().getPlayer(p.getName()) != null) {
	  			Player player = (Player) p;
	  			Util.coloredMessage(player, configUtils.lang("FREEZE_UNFROZEN"));
	  		}

		} else {
	  		frozenPlayerList.add(p.getUniqueId());
	  		Util.coloredMessage((Player)sender, "&c&n"+args[0]+"&c has been frozen!");

	  		if(Bukkit.getServer().getPlayer(p.getName()) != null) {
	  			Player player = (Player) p;

	  			if(Messages != null && Messages.size() > 0) {
	  				Messages.forEach(msg -> Util.coloredMessage(player, ConfigUtils.replaceVariable(msg)));
	  			}

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
