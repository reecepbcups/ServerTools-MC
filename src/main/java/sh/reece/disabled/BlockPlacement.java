package sh.reece.disabled;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;
import sh.reece.utiltools.Util;

public class BlockPlacement extends ToggleableListener {

	private static final Set<UUID> allowed_to_place = new HashSet<UUID>();

	public BlockPlacement(Main instance) {
		super(instance, "Disabled.DisableBlockPlacement");
	}


	@EventHandler(ignoreCancelled = true)
	public void onBlockBlock(BlockPlaceEvent e) {
		Player player = e.getPlayer();
		UUID uuid = player.getUniqueId();

		if(hasPermission(player)) { // can place
			if(!allowed_to_place.contains(uuid)){
				allowed_to_place.add(uuid);
				Util.coloredMessage(player, "&f&lSERVERTOOLS &8» &cDue to being staff, you can place blocks here");
			}
		} else {
			Util.coloredMessage(player, "&cBlock placement has been disabled");
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		allowed_to_place.remove(e.getPlayer().getUniqueId());
	}
}
