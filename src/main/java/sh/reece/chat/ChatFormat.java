package sh.reece.chat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;

import me.clip.placeholderapi.PlaceholderAPI;
import sh.reece.tools.Main;
import net.milkbowl.vault.chat.Chat;
import sh.reece.utiltools.Util;

public class ChatFormat implements Listener {

	

	private String format, ColorCodePerm;
	private Chat vaultChat = null;
	private int PrefixOffset;
	private boolean EnabledPAPIinMessages;
	
	private Main plugin;
	public ChatFormat(Main instance) {
		plugin = instance;

		EnabledPAPIinMessages = false; 

		if(plugin.enabledInConfig("Chat.ChatFormat.Enabled")) {
			
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);			
			
			// default should be 0
			PrefixOffset = plugin.getConfig().getInt("Chat.ChatFormat.PrefixOffset");
			ColorCodePerm = plugin.getConfig().getString("Chat.ChatFormat.ChatColorPerm");
			
			
			if(plugin.isPAPIEnabled()) {
				EnabledPAPIinMessages = plugin.getConfig().getBoolean("Chat.ChatFormat.EnabledPAPIinMessages");
			}
			
			reloadConfigValues();
			refreshVault();
		}
	}
	
	private void reloadConfigValues() {
		// add deluxetag support ehrre
	    this.format = colorize(plugin.getConfig().getString("Chat.ChatFormat.format")//, "{prefix}{name}{suffix} &7> &f{message}")
	        .replace("{name}", "%1$s")
	        .replace("{message}", "%2$s")); 
	  }
	  
	  private void refreshVault() {
	    Chat vaultChat = (Chat) Bukkit.getServer().getServicesManager().load(Chat.class);
	    if (vaultChat != this.vaultChat)
	    	Bukkit.getLogger().info("New Vault Chat implementation registered: " + ((vaultChat == null) ? "null" : vaultChat.getName())); 
	    this.vaultChat = vaultChat;
	  }
	  
	  @EventHandler
	  public void onServiceChange(ServiceRegisterEvent e) {
	    if (e.getProvider().getService() == Chat.class)
	      refreshVault(); 
	  }
	  
	  @EventHandler
	  public void onServiceChange(ServiceUnregisterEvent e) {
		  if (e.getProvider().getService() == Chat.class)
			  refreshVault(); 
	  }
	  
	  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	  public void onChatLow(AsyncPlayerChatEvent e) {
		  String f = this.format;
		  if(e.getPlayer().hasPermission(ColorCodePerm) || e.getPlayer().isOp()) {
			  f = colorize(f);
		  }
		  e.setFormat(f);		  
	  }
	  
	  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	  public void onChatHigh(AsyncPlayerChatEvent e) {
	    String format = e.getFormat();
	    Player p = e.getPlayer();	    
	    
	    if (this.vaultChat != null && format.contains("{prefix}")) {
	    	String prefix = this.vaultChat.getPlayerPrefix(p);

	    	if(PrefixOffset != 0) { // removes extra space on 1.8, here now just in case			    		
				prefix = prefix.substring(0, prefix.length()-PrefixOffset);
	    	} 
	    	
	    	format = format.replace("{prefix}", colorize(prefix));
	    }

	    if (this.vaultChat != null && format.contains("{suffix}"))
	    	format = format.replace("{suffix}", colorize(this.vaultChat.getPlayerSuffix(p)));

	    // if chatcolor.use, format chatcolor
	    if(e.getPlayer().hasPermission("chatcolor.codes") || e.getPlayer().isOp()) {
	    	format = colorize(format);
	    } 
	    
	    // if chat.placeholders, allow placeholders in their message
	    // allows for usage of: %server_max_players% in chat	   	  
	    if(EnabledPAPIinMessages) { 
	    	String message = e.getMessage();
	    	if(message.contains("%") && p.hasPermission("chat.placeholder.message")) {	    		
		    	message = PlaceholderAPI.setPlaceholders(p, message); 		    	
		    	e.setMessage(message);
	    	}	    	
	    }	 
	    
	    e.setFormat(format);	    
	  }

	 
	  
	  private static String colorize(String s) {
		// ChatColor.translateAlternateColorCodes('&', s)
	    return (s == null) ? null : Util.color(s);
	  }
	
	


}
