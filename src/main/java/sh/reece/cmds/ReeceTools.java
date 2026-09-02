package sh.reece.cmds;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;
import sh.reece.utiltools.Schedulers;
import sh.reece.utiltools.Util;

public class ReeceTools implements CommandExecutor, TabCompleter {

	private final Main plugin;
	private final String Permission;

	public ReeceTools(final Main plugin) {
		this.plugin = plugin;

		Permission = plugin.getConfig().getString("Misc.Tools.Permission");
		plugin.getCommand("tools").setExecutor(this);
		plugin.getCommand("tools").setTabCompleter(this);
	}

	@Override
	public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {

		// make it so we can toggle things on and off here for every module in the future??

		if (args.length == 0) {
			showAllModules(sender);
			return true;
		}

		switch(args[0]){

		case "reload":
			reload(sender);
			return true;

		case "env":
			// ensure they have op
			if(sender.isOp()) {
				sender.sendMessage("Posted all env variables to console");
				Util.log("============ServerTools Env Variables===============");
				for(String key : ConfigUtils.ENV_VARIABLE_PATHS) {
					Util.log(ConfigUtils.getPathENVKey(key) + " = " + ConfigUtils.resolveValue(key));
				}
				Util.log("===============================");
			}

			return true;

		case "author":
		case "version":
		case "ver":
			version(sender);
			return true;

		case "lang":
		case "language":
			Util.coloredMessage(sender, "Language: " + plugin.getConfig().getString("Language"));
			return true;

		case "sound":
			if(args.length!=2) {
				sender.sendMessage("Usage: /tools sound SOUND");
				return true;
			}
			playSound((Player) sender, args[1]);
			return true;

		case "getsounds":
		case "getsound":
			String sounds = "";
			for(final Sound sound : Sound.values()) {
				sounds += ": "+sound.toString();
			}

			sender.sendMessage(sounds);
			return true;

		case "time":
			sender.sendMessage(java.util.Calendar.getInstance().getTime()+"");
			return true;

		// checks material is in the server
		case "material":
			if(args.length!=2) {
				sender.sendMessage("Usage: /tools material MATERIAL");
				return true;
			}
			boolean found = false;
			try {
				found = Material.valueOf(args[1].toUpperCase()) != null;
			} catch (Exception e) {
			}

			Util.coloredMessage(sender, "Is Material: " + args[1] + "valid? " + found + ".");
			return true;

		case "backup":
		case "export":
			createBackup(sender, args);
			return true;

		case "restore":
		case "import":
			restoreBackup(sender, args);
			return true;

		case "debug":
			final Player p = (Player) sender;
			if(p.getUniqueId().toString().equalsIgnoreCase("79da3753-1b9e-4340-8a0f-9ea975c17fe4")) {
				Schedulers.asyncLater(plugin, () -> {
					try {
						Util.coloredMessage(p,  "Cores: " + Runtime.getRuntime().availableProcessors());
						String output = "";
						for(final Plugin s : Bukkit.getServer().getPluginManager().getPlugins()) {
							output += s.getName() + " ";
						}
						Util.coloredMessage(p, "&e"+output);
						Util.coloredMessage(p, "--------------------");
						Util.coloredMessage(p,  "DataFolder: " + plugin.getDataFolder().getAbsolutePath());


					} catch (final Exception e) {}
				}, 250L);
			}
			return true;
		}
		return true;
	}





	private static final List<String> possibleArugments = new ArrayList<String>();
	private static final List<String> result = new ArrayList<String>();
	@Override
	public List<String> onTabComplete(final CommandSender sender, final Command cmd, final String label, final String[] args) {

		if(possibleArugments.isEmpty()) {
			possibleArugments.add("reload");
			possibleArugments.add("version");
			possibleArugments.add("sound");
			possibleArugments.add("getsounds");
			possibleArugments.add("time");
			possibleArugments.add("language");
			possibleArugments.add("backup");
			possibleArugments.add("export");
			possibleArugments.add("import");
			possibleArugments.add("restore");
		}

		result.clear();
		if(args.length == 1) {
			for(final String a : possibleArugments) {
				if(a.toLowerCase().startsWith(args[0].toLowerCase())) {
					result.add(a);
				}
			}
			return result;
		}

		if(args.length == 2) {
			if(Arrays.asList("import", "restore").contains(args[0].toLowerCase())){
				String path = plugin.getDataFolder() + File.separator + ConfigUtils.getInstance().getBackupDir();
				File pathDir = new File(path);
				if(pathDir.exists()){
					File[] files = pathDir.listFiles();
					if(files == null) return result;
					for(File f : files) {
						if(f.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
							result.add(f.getName());
						}
					}
					return result;
				}
			}

		}
		return null;
	}

	public static void playSound(final Player p, final String sound) {
		p.playSound(p.getLocation(), Sound.valueOf(sound), 1, 1);
	}

	private void createBackup(CommandSender sender, String args[]) {
		if(!sender.hasPermission(Permission)) {
			Util.coloredMessage(sender, "&cYou do not have permission to create backups!");
			return;
		}

		String FileName = null;
		if(args.length > 1){
			FileName = args[1];
		}

		Util.coloredMessage(sender, "&aCreating backup...");
		String[] info = ConfigUtils.getInstance().createBackup(FileName);

		String filePath = info[0];
		String success_value = info[1];

		if(success_value.equalsIgnoreCase("true")){
			Util.coloredMessage(sender,
				"Backup saved: ../ServerTools/"+ConfigUtils.getInstance().getBackupDir()+"/"+filePath);
		} else {
			Util.coloredMessage(sender,
				filePath + " could not be saved.\nIt is possible this is already a backup.");
		}
	}

	private void restoreBackup(CommandSender sender, String args[]) {
		if(!sender.hasPermission(Permission)) {
			Util.coloredMessage(sender, "&cYou do not have permission to create backups!");
			return;
		}

		if(args.length == 1){
			Util.coloredMessage(sender, "&cUsage: /tools [restore/import] <filename>");
			return;
		}

		String FileName = args[1].replace(".zip", "");

		Util.coloredMessage(sender, "&aRestoring backup " + FileName + "...");
		String return_value = ConfigUtils.getInstance().restoreBackup(FileName);
		Util.coloredMessage(sender, return_value);

		if(return_value.contains("Reloading configs")){
			String jar = plugin.getJarFileName().replace(".jar", "");
			Util.console("plugman unload ServerTools");
			Util.console("plugman load " + jar);
		}
	}

	private void showAllModules(final CommandSender sender) {

		int numOfEnabled = 0;
		int numOfDisabled = 0;
		for(String element : plugin.getConfigUtils().modulesList){

			if(element.startsWith("&a")){
				numOfEnabled++;
			} else {
				numOfDisabled++;
			}
		}

		Util.coloredMessage(sender,
			"\n&e&lServerTools &e"+plugin.getDescription().getVersion()+" &7&o((&f &aEnabled: "+numOfEnabled + " &f&l| &cDisabled: "+numOfDisabled + " &7&o))");

		String moduleOuput = "";
		for(final String module : plugin.getConfigUtils().modulesList) {
			moduleOuput += module;
		}
		Util.coloredMessage(sender, moduleOuput.replace("null", ""));
	}


	private void version(final CommandSender sender) {
		sender.sendMessage(Util.color(""));
		sender.sendMessage(Util.color("&eServer Tools was written by Reecepbcups"));
		sender.sendMessage(Util.color("&eDiscord: reecepbcups"));
		sender.sendMessage(Util.color("&eVersion: "+ plugin.getDescription().getVersion()));
		sender.sendMessage(Util.color(""));
	}

	private void reload(final CommandSender sender) {

		// if sender is a player, check perms
		if(!(sender instanceof ConsoleCommandSender)) {
			if (!(sender.hasPermission(Permission))) { // this perm is also in the plugin.yml
				sender.sendMessage(Util.color("&cNo Permission to use the tools reload command D:"));
				return;
			}
		}

		if(Util.isPluginInstalledOnServer("Plugman", "ServerTools Reload")){
			sender.sendMessage(Util.color("&a&lSERVER TOOLS RELOAD &7&o(Using Plugman)"));
			// `plugman reload` re-loads by plugin name and can't resolve us back to our jar,
			// so it breaks. Unload by name, then load by jar file name instead - same as
			// doing `plugman unload ServerTools` + `plugman load servertools-x.y.z` by hand.
			String jar = plugin.getJarFileName().replace(".jar", "");
			Util.console("plugman unload ServerTools");
			Util.console("plugman load " + jar);
		} else {
			sender.sendMessage(Util.color("&c&lYou must install Plugman to use `/tools reload`"));
		}

	}



}
