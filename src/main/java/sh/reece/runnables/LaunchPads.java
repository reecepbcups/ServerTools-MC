package sh.reece.runnables;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class LaunchPads implements Listener, CommandExecutor {

	private Main plugin;
	private Material BlockType, PlateType;
	private int LaunchPower;

	public LaunchPads(Main instance) {
		plugin = instance;

		String Section = "Events.Launchpads";
		if (plugin.getConfigUtils().enabledInConfig(Section + ".Enabled")) {
			FileConfiguration config = plugin.getConfig();

			try {
				BlockType = Material.valueOf(config.getString(Section + ".BlockType").toUpperCase());
				PlateType = Material.valueOf(config.getString(Section + ".PlateType").toUpperCase());
			} catch (Exception e) {
				BlockType = Material.EMERALD_BLOCK;
				PlateType = Material.STONE_PRESSURE_PLATE;
				Util.log("[LaunchPads] BlockType or PlateType not found in config / not supported. Using defaults.");
			}

			LaunchPower = config.getInt(Section + ".LaunchPower");

			plugin.getCommand("launchpad").setExecutor(this);
			plugin.getServer().getPluginManager().registerEvents(this, plugin);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onMove(PlayerMoveEvent e) {
		if (e.getTo() == null) return;
		if (e.getFrom().getBlockX() == e.getTo().getBlockX()
			&& e.getFrom().getBlockY() == e.getTo().getBlockY()
			&& e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;

		Location loc = e.getTo();
		if (loc.getBlock().getType() != PlateType) return;
		if (loc.getWorld().getBlockAt(loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ()).getType() != BlockType) return;

		Player p = e.getPlayer();
		Vector dir = loc.getDirection().multiply(LaunchPower);
		p.setVelocity(new Vector(dir.getX(), 1.0D, dir.getZ()));
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender.hasPermission("launchpad.admin"))) {
			sender.sendMessage(Util.color("&cNo Permission to use " + label + " :("));
			return true;
		}

		Player p = (Player) sender;

		if (args.length == 0) {
			sendHelpMenu(p);
			return true;
		}

		switch (args[0]) {
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
