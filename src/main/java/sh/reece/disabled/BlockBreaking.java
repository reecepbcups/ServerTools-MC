package sh.reece.disabled;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class BlockBreaking implements Listener {

	private static Main plugin;
	private static final Set<UUID> allowed_to_break = new HashSet<UUID>();
	private String permission;

	public BlockBreaking(Main instance) {
        plugin = instance;

        if (plugin.enabledInConfig("Disabled.DisableBlockBreaking.Enabled")) {
    		Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
			permission = plugin.getConfig().getString("Disabled.DisableBlockBreaking.Permission");
    	}
	}
	
	
	@EventHandler(ignoreCancelled = true)
	public void onBlockBlock(BlockBreakEvent e) {
		Player player = e.getPlayer();
		UUID uuid = player.getUniqueId();
		
		if(player.hasPermission(permission)) { // can break
			if(!allowed_to_break.contains(uuid)){
				allowed_to_break.add(uuid);
				Util.coloredMessage(player, "&f&lSERVERTOOLS &8» &aDue to being staff, you can break blocks here");
			} 

		} else {
			Util.coloredMessage(player, "&cBlock breaking has been disabled");
			e.setCancelled(true);
		}

	}
	
}
