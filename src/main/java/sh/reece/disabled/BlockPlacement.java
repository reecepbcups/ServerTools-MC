package sh.reece.disabled;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class BlockPlacement implements Listener {

	private static Main plugin;
	private static final Set<UUID> allowed_to_place = new HashSet<UUID>();
	private String permission;

	public BlockPlacement(Main instance) {
        plugin = instance;

        if (plugin.enabledInConfig("Disabled.DisableBlockPlacement.Enabled")) {
    		Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
			permission = plugin.getConfig().getString("Disabled.DisableBlockPlacement.Permission");
    	}
	}
	
	
	@EventHandler(ignoreCancelled = true)
	public void onBlockBlock(BlockPlaceEvent e) {
		Player player = e.getPlayer();
		UUID uuid = player.getUniqueId();

		if(player.hasPermission(permission)) { // can break
			if(!allowed_to_place.contains(uuid)){
				allowed_to_place.add(uuid);
				Util.coloredMessage(player, "&f&lSERVERTOOLS &8» &cDue to being staff, you can place blocks here");
			}
		} else {
			Util.coloredMessage(player, "&cBlock placement has been disabled");
			e.setCancelled(true);
		}
	}
	
}
