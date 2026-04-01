package sh.reece.aFinishLater;

import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class DiscordBugReport implements Listener {

	//private static Main plugin;
	private String command, WebhookURL,BotName,BotDesc,BotTitle, BotPicture, CooldownMSG, ReportSuccess;
	private Integer cooldownSeconds;
	private HashMap<String, Long> CooldownHash;
	 
	public DiscordBugReport(Main instance) {
	        //plugin = instance;
	        
	        // Currently Removed bc - not really needed.
	        
//	        if (plugin.EnabledInConfig("Misc.DiscordBugReports.Enabled")) {				
//				Bukkit.getServer().getPluginManager().registerEvents(this, plugin);			
//				command = "/"+plugin.getConfig().getString("Misc.DiscordBugReports.Command");
//				WebhookURL = plugin.getConfig().getString("Misc.DiscordBugReports.WebhookURL");
//				
//				BotName = plugin.getConfig().getString("Misc.DiscordBugReports.Bot.name");
//				BotTitle = plugin.getConfig().getString("Misc.DiscordBugReports.Bot.title");
//				BotDesc = plugin.getConfig().getString("Misc.DiscordBugReports.Bot.desc");
//				BotPicture = plugin.getConfig().getString("Misc.DiscordBugReports.Bot.thumbnail");
//				
//				
//				cooldownSeconds = plugin.getConfig().getInt("Misc.DiscordBugReports.cooldown");
//		        CooldownHash = new HashMap<String, Date>();
//		        CooldownMSG = plugin.getConfig().getString("Misc.DiscordBugReports.cooldownmsg");
//		        
//		        ReportSuccess = plugin.getConfig().getString("Misc.DiscordBugReports.ReportedSuccesful");
//				
//			}
	}

	// config.yml
/*
	  DiscordBugReports:
		    Enabled: false
		    Command: bug
		    cooldown: 120
		    cooldownmsg: "&6&l[!] &eYou must wait &6&n%timeleft%s&e before you may report another bug."
		    ReportedSuccesful: "&aSUCCESS! &fWe have recived your report thank you!"
		    WebhookURL: "https://discord.com/api/webhooks"
		    Bot:
		      name: "Fantasy Bugs"
		      title: "Fantasy Network"
		      desc: "Bug Report"
		      thumbnail: http://fantasynetwork.co/fantasy_files/server-icon.png
*/	
	
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onCommand(PlayerCommandPreprocessEvent e) throws IOException {
        
        if (!e.getMessage().toLowerCase().startsWith(command)){
            return;
        }
        
        e.setCancelled(true);
        
        Player p = e.getPlayer();
        
        //checks to make sure the URL is not just basic default
        if (WebhookURL.equalsIgnoreCase("https://canary.discordapp.com/api/webhooks")){
        	p.sendMessage("[Tools:DicordBugReports] has not been configured properly");
            return;
        }

        if(!(e.getMessage().split(" ").length > 1)) {
        	p.sendMessage("You neeed to provide more information for this report!");
        	return;
        }
        
        if(!(Util.cooldown(CooldownHash, cooldownSeconds, p.getName(), CooldownMSG))) {
    		e.setCancelled(true);     
    		return;
    	}
        
        // http://fantasynetwork.co/fantasy_files/me.png

        DISCORD_WEBHOOK webhook = new DISCORD_WEBHOOK(WebhookURL);
        //webhook.setContent("Any message!");
        //webhook.setAvatarUrl("https://your.awesome/image.png");
        webhook.setUsername(BotName);
        webhook.setTts(true);
        webhook.addEmbed(new DISCORD_WEBHOOK.EmbedObject()
                //.setTitle("Title")
                .setDescription(BotDesc)
                .setColor(Color.RED)
                
                   
                
        .addField("Username ", p.getName(), true)
        .addField("World ", p.getWorld().getName(), false)
        .addField("BUG", e.getMessage().replaceFirst(command, ""), false)
        
        .setThumbnail(BotPicture)
        .setFooter("", "")
        //.setImage("https://kryptongta.com/images/kryptontitle2.png")
        .setAuthor(BotTitle, "", BotPicture));
        //.setUrl("https://kryptongta.com"));
        
        //webhook.addEmbed(new DISCORD_WEBHOOK.EmbedObject().setDescription("Just another added embed object!"));
        
        webhook.execute(); //Handle exception       
        
        p.sendMessage(Util.color(ReportSuccess));
        e.setCancelled(true);
	
	}
	
	

	
	
}
