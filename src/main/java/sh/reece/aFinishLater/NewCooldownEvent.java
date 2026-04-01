package sh.reece.aFinishLater;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class NewCooldownEvent implements Listener {

	private static Main plugin;
	
	// <GOLDEN_APPLE, [data, seconds, message, startCooldownMSG]>
	private static final HashMap<Material, List<String>> CooldownData = new HashMap<Material, List<String>>();
	
	//cooldowns based on the material type
	private static final HashMap<Material, HashMap<String, Date>> CooldownStorage = new HashMap<Material, HashMap<String, Date>>();
	
	public NewCooldownEvent(Main instance) {
		plugin = instance;
        
        final String section = "Cooldowns.PlayerItemConsumeEvent";
        
        
        
        // Work in progress, doesnt work sadly due to LEGACY_ issues
        
        /*
         *   
  PlayerItemConsumeEvent:
    Enabled: false
    GOLDEN_APPLE:
      data: 1
      seconds: 500
      message: '&6&l[!] &eYou must wait &6&n%seconds%s&e before you may eat another golden apple.'
      StartCooldownMSG: '&7&o(( GOLDEN EATEN! %seconds% second cooldown initiated ))'
    COOKED_BEEF:
      data: 0
      seconds: 120
      message: '&6&l[!] &eYou must wait &6&n%seconds%s&e before you may eat another steak.'
      StartCooldownMSG: '&7&o(( STEAK EATEN! %seconds% second cooldown initiated ))'
    ROTTEN_FLESH:
      data: 0
      seconds: 60
      message: '&6&l[!] &eYou must wait &6&n%seconds%s&e before you may eat another flesh.'
      StartCooldownMSG: '&7&o(( FLESH EATEN! %seconds% second cooldown initiated ))'
    STEAK:
      data: 0
      seconds: 60
      message: '&6&l[!] &eYou must wait &6&n%seconds%s&e before you may eat another flesh.'
      StartCooldownMSG: '&7&o(( FLESH EATEN! %seconds% second cooldown initiated ))'
         */
        
        
        if (plugin.getConfigUtils().enabledInConfig(section+".Enabled")) {
        	
        	FileConfiguration config = plugin.getConfig();
        	List<String> keyData;
        	Material mat;
        	
        	for(String KEY : config.getConfigurationSection(section).getKeys(false)) {

        		if(!KEY.equalsIgnoreCase("enabled")) {
        			//Util.consoleMSG("Cooldown " + KEY.toString());
        			
        			keyData = new ArrayList<String>();
            		String keyPath = section+ "" +KEY+ "";
        			
            		keyData.add(config.getString(keyPath+".data")); // Cooldowns.PlayerItemConsumeEvent.KEY.data
            		keyData.add(config.getString(keyPath+".seconds"));
            		keyData.add(config.getString(keyPath+".message"));
            		keyData.add(config.getString(keyPath+".StartCooldownMSG"));
            		mat = Material.valueOf(KEY.replace("LEGACY_", ""));
            		
            		// <GOLDEN_APPLE, [data, seconds, message, startCooldownMSG]>            		
            		CooldownData.put(mat, keyData);
            		
            		CooldownStorage.put(mat, new HashMap<String, Date>());
            		
        		}
        		
        	}
        	
        	//Util.consoleMSG("Hash Keys " + CooldownData.keySet().toString());
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);			
		}
	}
	
	
	@EventHandler
	@SuppressWarnings( "deprecation" )
	public void Gapple_Cooldown(PlayerItemConsumeEvent e) {
		Player p = e.getPlayer();
		ItemStack TYPE = e.getItem();
		Material mat = Material.valueOf(TYPE.getType().toString().replace("LEGACY_", ""));


		// Util.consoleMSG("Eaten Data: "+ TYPE.getData().getData());
		
		// if hashmap key is a value we use for cooldowns
		if(CooldownData.containsKey(mat)) {

			Util.consoleMSG("Hash contains " + mat.toString() + " value. Checking if data matches");
			if(TYPE.getData().getData() == Byte.valueOf(CooldownData.get(mat).get(0))) {
				
				Util.consoleMSG("Data Matches!");
				if(cooldown(mat, Integer.valueOf(CooldownData.get(mat).get(1)), p.getName(), CooldownData.get(mat).get(2))) {
					Util.consoleMSG("Event canceld");
					e.setCancelled(true);        	
				} 	
			}
			
			

					
		}
	}
	
	
	
	public static boolean cooldown(Material mat, Integer SecondCooldown, String PlayerName, String CooldownMessage) {
		
		long CURRENT_TIME = new Date().getTime();

		Player p = Bukkit.getServer().getPlayer(PlayerName);
		
		if(!(CooldownStorage.get(mat).containsKey(PlayerName))) {	
			long mil_cooldown = SecondCooldown * 1000;
			CooldownStorage.get(mat).put(PlayerName , new Date(CURRENT_TIME + mil_cooldown));	
			
			//String temp = ;
			Util.coloredMessage(p, CooldownData.get(mat).get(3).replace("%seconds%", SecondCooldown+""));
			return false;
		}
		
		if (CooldownStorage.get(mat).containsKey(PlayerName) && CooldownStorage.get(mat).get(PlayerName).getTime() >= CURRENT_TIME) {			
			
			
			Long timeLeft = ((CooldownStorage.get(mat).get(PlayerName).getTime() - CURRENT_TIME) / 1000);
			p.sendMessage(Util.color(CooldownMessage.replace("%seconds%", timeLeft.toString())));
			//Util.console("bc return false. On cooldown");
			return true;

		} else {
			CooldownStorage.get(mat).remove(PlayerName); //Cooldown is over			
		}

		//CooldownStorage.put(mat, CooldownStorage.get(mat));
		return false;				
	}
	
	
	
//	private HashMap<String, Date> CooldownHash;
	//		
	//		// inside of the Constructor
	//		this.CooldownHash = new HashMap<String, Date>();

	//		Integer CooldownSeconds = 5;		
	//		if(!(Util.cooldown(CooldownHash, CooldownSeconds, p))) {
	//			// User has cooldown	  
	//    		e.setCancelled(true);
	//    	}

	
	
//  Cooldowns:		
//		  PlayerInteractEvent:
//		    ENDER_PEARL:
//            data: 0
//            seconds: 15
//		      Actions:
//		      - RIGHT_CLICK_BLOCK
//		      - RIGHT_CLICK_AIR
	
	
//	if(!(Util.cooldown(CooldownHash, cooldownSeconds, p.getName(), CooldownMSG))) {
//		e.setCancelled(true);        	
//	}   
	

	
	
}
