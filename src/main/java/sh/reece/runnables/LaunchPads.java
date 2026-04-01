package sh.reece.runnables;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class LaunchPads implements Listener, CommandExecutor {

	private static Main plugin;
	private FileConfiguration config;
	private String Section;
	private Material BlockType, PlateType;
	private Integer LaunchPower, RunnableTicksperCheck;
	private static boolean isEnabled;
	
	public LaunchPads(Main instance) {
        plugin = instance;
        
        Section = "Events.Launchpads";                
        if(plugin.enabledInConfig(Section+".Enabled")) {
        	isEnabled = true;
        	
        	//config = plugin.getConfig();
        	config = plugin.getConfig();

			try {
				BlockType = Material.valueOf(config.getString(Section+".BlockType").toUpperCase());
        		PlateType = Material.valueOf(config.getString(Section+".PlateType").toUpperCase());
			} catch (Exception e) {
				BlockType = Material.EMERALD_BLOCK;
				PlateType = Material.STONE_PRESSURE_PLATE;
				Main.logging("[LaunchPads] BlockType or PlateType not found in config / not supported. Using defaults.");
			}        			

        	//SoundEffect = config.getString(Section+".SoundEffect");
        	LaunchPower = config.getInt(Section+".LaunchPower");
        	RunnableTicksperCheck = config.getInt(Section+".RunnableSecondsCheck");
        	
        	plugin.getCommand("launchpad").setExecutor(this);
    		
        	runLaunchPadChecker();
    	}
	}
	
	static int launchpadRunnable;
	public void runLaunchPadChecker() {

		launchpadRunnable = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			public void run() {
				for(Player p : Bukkit.getOnlinePlayers()) {
					Location loc = p.getLocation();
					if(loc.getBlock().getType() == PlateType) {
						if(loc.clone().subtract(0, 1, 0).getBlock().getType() == BlockType) {
							p.setVelocity(loc.getDirection().multiply(LaunchPower));
							p.setVelocity(new Vector(p.getVelocity().getX(), 1.0D, p.getVelocity().getZ()));
						}
					}
				}
			}
		}, 0, RunnableTicksperCheck);
	}
	public static void stopLaunchpadChecking() {
		if(isEnabled) {
			Bukkit.getServer().getScheduler().cancelTask(launchpadRunnable);
		}		
	}

	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {		
		if (!(sender.hasPermission("launchpad.admin"))) {		
			sender.sendMessage(Util.color("&cNo Permission to use "+label+" :("));
			return true;			
		}
		
		Player p = (Player) sender;

		if (args.length == 0) {
			sendHelpMenu(p);
			return true;
		}	
		
		switch(args[0]){
			// /command clear
			case "create":				
				p.getLocation().getWorld().getBlockAt(p.getLocation()).getRelative(0, -1, 0).setType(BlockType);
				p.getLocation().getWorld().getBlockAt(p.getLocation()).setType(PlateType);
				return true;
			default:
				sendHelpMenu(p);
				return true;		
		}		
	}
	
	public void sendHelpMenu(Player p) {
		Util.coloredMessage(p, "&f/launchpad &7create");
	}
	
	
	
}
