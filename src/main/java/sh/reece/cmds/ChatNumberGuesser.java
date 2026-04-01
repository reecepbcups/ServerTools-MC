package sh.reece.cmds;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;
import sh.reece.utiltools.Util;

public class ChatNumberGuesser extends ToggleableListener {

	private String command, AdminPerm;

	public Boolean Running;
	public Integer Number;

	public ChatNumberGuesser(Main instance) {
		super(instance, "Commands.ChatNumberGuess");

		if(isEnabled()) {
			command = "/"+instance.getConfig().getString("Commands.ChatNumberGuess.command");
			AdminPerm = instance.getConfig().getString("Commands.ChatNumberGuess.AdminPerm");
			Running = false;
		}
	}


	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onCommand(PlayerCommandPreprocessEvent e) {

		//e.getPlayer().sendMessage(e.getMessage());

		if (!e.getMessage().toLowerCase().startsWith(command)){
			return;
		}

		Player p = e.getPlayer();

		if (!(p.hasPermission(AdminPerm))) {
			p.sendMessage(Util.color("&cNo Permission " + AdminPerm));
			return;
		}
		e.setCancelled(true);

		String[] args = e.getMessage().replace(command, "").split(" ");

		//p.sendMessage(args.length + "");

		if(args.length <= 1) {
			Util.coloredMessage(p, "&cUSAGE: /guess (start/stop)");
			return;
		}

		//p.sendMessage(args);

		if(args[1].equalsIgnoreCase("start")) {
			if(Running) {
				Util.coloredMessage(p, "&cGame already running!");
				return;
			}

			if(args.length > 0) {
				Number  = new Random().nextInt(100) + 1;
				Util.coloredMessage(p, "&7&o(( random number " + Number + " chosen ))");

				Running = true;


			}
			Bukkit.broadcastMessage("");
			Bukkit.broadcastMessage(Util.color(" &fPick a number between &a1 &f- &a100!"));
			Bukkit.broadcastMessage("");
		}

		if(args[1].equalsIgnoreCase("stop")) {
			if(Running) {
				Bukkit.broadcastMessage(Util.color("  &cThe chat guess game has been stopped by " + p.getName()));
			}



		}


	}



	@EventHandler(ignoreCancelled = true)
	public void onChat(AsyncPlayerChatEvent e) {
		if (!Running) {
			return;
		}

		if (e.getMessage().equalsIgnoreCase(String.valueOf(Number))) {
			Running = false;

			String msg = "&c%player% has won the guessing game! The number was %number%.";
			msg = msg.replace("%player%", e.getPlayer().getName());
			msg = msg.replace("%number%", Number.toString());
			Bukkit.broadcastMessage(Util.color(msg));
		}
	}









}
