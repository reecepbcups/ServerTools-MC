package sh.reece.events;

import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.ArrayList;
import java.util.List;

public class Withdraw implements Listener, CommandExecutor {

	private static Main plugin;
	private final String Section;
	public String noteName = Util.color("&a&lCash-Note &7(Right-Click)");
	public List<String> lore = new ArrayList<>();
	//private static boolean Vault;	  
	private static Economy econ = null;
	private boolean debug;
	
	private ConfigUtils configUtils;

	public Withdraw(Main instance) {
		plugin = instance;

		Section = "Misc.Withdraw";     
		
		if(plugin.enabledInConfig(Section+".Enabled")) {
			configUtils = plugin.getConfigUtils();
			//Vault = setupEco();
			setupEco();
			
			lore.add(configUtils.lang("WITHDRAW_NOTE_LORE1"));
			lore.add(configUtils.lang("WITHDRAW_NOTE_LORE2"));

			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
			plugin.getCommand("withdraw").setExecutor(this);
			debug = false;
		}
	}

	private boolean setupEco() {
		if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = (Economy)rsp.getProvider();
		return (econ != null);
	}


	@EventHandler(ignoreCancelled = true)
	private void noteRedeem(PlayerInteractEvent e) {
		Player p = e.getPlayer();
		if (e.getMaterial() == null) {
			return;
		}
		if (!e.getMaterial().equals(Material.PAPER)) {
			return;
		}
		
		ItemStack note = e.getItem();
		if(!note.hasItemMeta()) {
			return;
		}
		if(!note.getItemMeta().hasDisplayName()) {
			return;
		}
		if(!note.getItemMeta().hasLore()) {
			return;
		}
		
		if (note.getItemMeta().getDisplayName().equals(noteName)) {
			ItemMeta im = note.getItemMeta();
			if (!im.hasLore()) {
				return;
			}
			
			// strips chat color stuff to correctly format data to get the $ amount on the note in lore
			String value = ChatColor.stripColor(note.getItemMeta().getLore().get(0).replace("$", "__d__"));
			Long amount = Long.parseLong(value.split("__d__")[1]);
			//Util.consoleMSG(value);			
	
			String finalOutput = configUtils.lang("WITHDRAW_MONEY") + Util.formatNumber(amount);
			int noteAMT = note.getAmount();
			
			if(p.isSneaking()) {	// if player sneaks, let them open more notes							
				p.getInventory().setItem(p.getInventory().getHeldItemSlot(), null);
				econ.depositPlayer((OfflinePlayer)p, amount*noteAMT);
				Util.coloredMessage(p,
						finalOutput+"&r &f&lx"+noteAMT+" &7&o(( $"+Util.formatNumber(amount*noteAMT)+" ))");
				
				debugStatement("&e"+p.getName()+" redeemed "+noteAMT+" notes @$"+amount+" per via shift clicking ");

			} else {
				econ.depositPlayer((OfflinePlayer)p, amount);
				Util.coloredMessage(p, finalOutput);
			}

			Util.removeItemFromPlayer(p, note, 1);
			e.setCancelled(true);
		} 
	}
	

	// bal command
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onCommand(PlayerCommandPreprocessEvent e) {

		if (!e.getMessage().toLowerCase().startsWith("/bal")){
			return;
		}
		
		if (e.getMessage().toLowerCase().startsWith("/baltop")){
			return;
		}
		if(e.getMessage().split(" ").length != 1) {
			return;
		}        
		e.setCancelled(true);

		Player p = e.getPlayer();
		String string_bal = Util.formatNumber(econ.getBalance((OfflinePlayer)p));

		p.sendMessage("");        
		Util.coloredMessage(p, configUtils.lang("WITHDRAW_BALANCE").replace("%bal%", string_bal));
		p.sendMessage("");        
	}

	// on withdraw
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		// give option to give a money note from console here
		if (sender instanceof ConsoleCommandSender) {

			// withdraw 1000 Player
			Long Amount = Long.parseLong(args[0]);
			Player p = Bukkit.getPlayer(args[1]);

			if(p == null) {
				sender.sendMessage("Player " + args[1] + " is not online");
				return true;
			} 
			p.getInventory().addItem(new ItemStack[] { makeNote(Amount, "CONSOLE") });
			return true;
		} 

		Player p = (Player) sender;

		if (args.length > 0) {
			if (args.length == 1) {
				try {
					Long Amount;

					if(args[0].equalsIgnoreCase("debug")) {
						debug = !debug;
						p.sendMessage("Debugging for withdraw: " + debug);
						return true;
					}

					if(args[0].equalsIgnoreCase("all")) {
						Amount = (long) econ.getBalance((OfflinePlayer)p);
						debugStatement("&c"+p.getName()+" withdrew all. $" + Amount);
					} else {
						Amount = Long.parseLong(args[0]);
						debugStatement("&c"+p.getName()+" parsed argument for $" + Amount);
					}

					double currentbal = econ.getBalance((OfflinePlayer)p);

					// if player does not have enough money, dont allow withdraw
					if(!(currentbal >= Amount)) {
						debugStatement("&c"+p.getName()+" You do not have enough money to withdraw $"+Amount);
						Util.coloredMessage(p, configUtils.lang("WITHDRAW_NOTENOUGH") + Util.formatNumber(Amount));
						return true;
					}

					if(Amount <= 0) {
						Util.coloredMessage(p, configUtils.lang("WITHDRAW_POSITIVE_NUMBER"));
						debugStatement("&c"+p.getName()+" tried to withdraw <= 0. ("+Amount+")");
						return true;
					}

					EconomyResponse response = econ.withdrawPlayer((OfflinePlayer)p, Amount);

					if (response.type.equals(EconomyResponse.ResponseType.SUCCESS)) {
						Util.coloredMessage(p, configUtils.lang("WITHDRAW_SUCCESSFUL") + Util.formatNumber(Amount));
						debugStatement("&a"+p.getName()+" withdrew "+Amount +" successfully");
						p.getInventory().addItem(makeNote(Amount, p.getName()));
					} else {
						Util.coloredMessage(p, "&aError");
						debugStatement("&a"+p.getName()+" did not withdaw successfuly...");
					}
				} catch (NumberFormatException e) {
					debugStatement("&a"+p.getName()+" did not enter a whole number...");
					Util.coloredMessage(p, configUtils.lang("WITHDRAW_ONLY_WHOLE"));
					return true;
				}
			}
		} else {
			Util.coloredMessage(p, "&e/withdraw <amount / all>");
			Util.coloredMessage(p, "&7Deposit your money to paper.");
			Util.coloredMessage(p, "&7Use &e/balance &7to view your current Cash.");
			
			if(p.isOp()) {
				Util.coloredMessage(p, "&e/withdraw debug &7(Console Output)");
			}
			
		} 
		return true;
	}

	private void debugStatement(String msg) {
		if(debug) {
			Util.consoleMSG("[Withdraw] " + msg);
		}
	}
	

	public ItemStack makeNote(Long Amount, String Signer) {

		List<String> updatedLore = new ArrayList<>();
		for(String s : lore) {
			s = s.replace("%value%", ""+Amount).replace("%player%", Signer);
			updatedLore.add(s);
		}	

		ItemStack is = new ItemStack(Material.PAPER, 1);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName(noteName);
		im.setLore(updatedLore);
		is.setItemMeta(im);
		return is;
	}



}
