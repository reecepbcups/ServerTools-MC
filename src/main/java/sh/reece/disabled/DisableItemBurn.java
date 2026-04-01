package sh.reece.disabled;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import sh.reece.tools.Main;

public class DisableItemBurn implements Listener {

	private List<EntityDamageEvent.DamageCause> causes = new ArrayList<>();

	private Main plugin;
	public DisableItemBurn(Main instance) {
		this.plugin = instance;

		if (plugin.enabledInConfig("Disabled.DisableItemBurn.Enabled")) {
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
			
//			causes.add(EntityDamageEvent.DamageCause.LAVA);
//			causes.add(EntityDamageEvent.DamageCause.FIRE);
//			causes.add(EntityDamageEvent.DamageCause.FIRE_TICK);
//			causes.add(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION);
//			causes.add(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION);
			
			for(String s : plugin.getConfig().getStringList("Disabled.DisableItemBurn.reasons"))
				causes.add(EntityDamageEvent.DamageCause.valueOf(s.toUpperCase()));
		}

	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onItemBurn(EntityDamageEvent e) {
		if (!e.isCancelled() && e.getEntity() instanceof Item && causes.contains(e.getCause())) {
			e.setCancelled(true); 
		}
			
	}
}
