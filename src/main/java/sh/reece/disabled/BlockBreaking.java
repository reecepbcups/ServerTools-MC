package sh.reece.disabled;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;
import sh.reece.utiltools.Util;

public class BlockBreaking extends ToggleableListener {

	private static final Set<UUID> allowed_to_break = new HashSet<UUID>();

	public BlockBreaking(Main instance) {
		super(instance, "Disabled.DisableBlockBreaking");
	}


	@EventHandler(ignoreCancelled = true)
	public void onBlockBlock(BlockBreakEvent e) {
		Player player = e.getPlayer();
		UUID uuid = player.getUniqueId();

		if(hasPermission(player)) { // can break
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
