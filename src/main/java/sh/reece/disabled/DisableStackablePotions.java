package sh.reece.disabled;


import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;

public class DisableStackablePotions implements Listener {

	private static Main plugin;
	private final String Section;
	private ConfigUtils configUtils;
	
	public DisableStackablePotions(Main instance) {
        plugin = instance;
        
       Section = "Disabled.DisableStackablePotions";                
       if(plugin.enabledInConfig(Section+".Enabled")) {
			configUtils = plugin.getConfigUtils();
    		Bukkit.getServer().getPluginManager().registerEvents(this, plugin);    		
    	}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void plashEvent(PotionSplashEvent e) {
		//Util.consoleMSG("PotionSplashEvent");		
		if(e.getPotion().getShooter() instanceof Player) {
			Player shooter = (Player) e.getPotion().getShooter();

			if(shooter.getInventory().getItemInHand().getAmount() > 1){
				Util.coloredMessage(shooter, configUtils.lang("DISABLED_STACKED_POTIONS"));
				e.setCancelled(true);
				
				// gives player their potion back
				ItemStack newStack = shooter.getInventory().getItemInHand().clone();
				newStack.setAmount(1);				
				shooter.getInventory().addItem(newStack);
			}
		}
		
	}
}
