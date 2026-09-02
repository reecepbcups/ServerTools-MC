package sh.reece.core;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import me.clip.placeholderapi.PlaceholderAPI;
import sh.reece.utiltools.Schedulers;
import sh.reece.utiltools.Util;
import sh.reece.tools.AlternateCommandHandler;
import sh.reece.tools.ConfigUtils;
import sh.reece.tools.Main;

public class Holograms implements CommandExecutor, Listener, TabCompleter {

	// on click of amour stand, if armour stand has text & armour stands are around up (y+ & -):
	// /minecraft:kill @e[type=armor_stand,r=3] remove those armour stands within that location

	private static Main plugin;
	private FileConfiguration HoloConfig;
	private String Section;
	private Boolean papiSupport;

	private Set<String> holoKeys;
	public HashMap<Location, Integer> EntitiyIDs = new HashMap<Location, Integer>();
	public DecimalFormat df = new DecimalFormat("#.###");
	private String permission;
	private Player randomPlayer;
	private ConfigUtils configUtils;

	public Holograms(Main instance) {
        plugin = instance;

        Section = "Misc.Holograms";
        randomPlayer = null;

        if(plugin.getConfigUtils().enabledInConfig(Section+".Enabled")) {

			configUtils = plugin.getConfigUtils();

        	papiSupport = plugin.isPAPIEnabled();

        	configUtils.createConfig("Holograms.yml");
        	loadInit();
        	permission = "hologram.admin";
        	plugin.getCommand("holograms").setExecutor(this);
        	plugin.getCommand("holograms").setTabCompleter(this);
    	} else {
			AlternateCommandHandler.addDisableCommand("holograms");
			AlternateCommandHandler.addDisableCommand("hologram");
			AlternateCommandHandler.addDisableCommand("holo");
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent e) {
		if(randomPlayer == null) {
			randomPlayer = e.getPlayer();

			Schedulers.globalLater(plugin, this::loadInit, 10L);
		}
	}


	public void loadInit() {
		HoloConfig = configUtils.getConfigFile("Holograms.yml");
		// load in all keys here for stands on startup
    	// on disable delete all armour stands at locations given in config
    	holoKeys = HoloConfig.getKeys(false); // [skyblock]

    	if(papiSupport) {
    		var online = Bukkit.getOnlinePlayers();
    		randomPlayer = online.isEmpty() ? null : online.iterator().next();
    	}

		// spawn holograms only if holokeys has more than 1
		if(holoKeys.size() >= 1){
			removeAllStands();
    		spawnAllHolos();
		}

	}

	private static final List<String> possibleArugments = Arrays.asList("create", "remove", "show", "hide", "list", "teleport", "removenear");
	private List<String> result = new ArrayList<String>();
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		result.clear();
		if(args.length == 1) {

			for(String a : possibleArugments) {
				if(a.toLowerCase().startsWith(args[0].toLowerCase())) {
					result.add(a);
				}
			}
			return result;
		}
		if(args[0].equalsIgnoreCase("remove") && args.length == 2) {
			for(String a : holoKeys) {
				if(a.toLowerCase().startsWith(args[1].toLowerCase())) {
					result.add(a);
				}
			}
			return result;
		}
		if((args[0].equalsIgnoreCase("teleport") || args[0].equalsIgnoreCase("tp")) && args.length == 2) {
			for(String a : holoKeys) {
				if(a.toLowerCase().startsWith(args[1].toLowerCase())) {
					result.add(a);
				}
			}
			return result;
		}
		if(args[0].equalsIgnoreCase("create") && args.length == 2) {
			for(String a : new String[] {"MyHologram"}) {
				if(a.toLowerCase().startsWith(args[1].toLowerCase())) {
					result.add(a);
				}
			}
			return result;
		}
		return null;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if(sender instanceof ConsoleCommandSender) {
			if(args.length == 1) {
				if(args[0].equalsIgnoreCase("reload")) {
					loadInit();
					Util.consoleMSG("Reloaded Holograms!");
				}
			} else {
				Util.consoleMSG("/holo reload");
			}
			return true;
		}

		if (!sender.isOp() && !sender.hasPermission(permission)) {
			sender.sendMessage(Util.color("&cNo Permission to use "+label+" :("));
			return true;
		}

		Player p = (Player) sender;

		if (args.length == 0) {
			sendHelpMenu(p);
			return true;
		}

		switch(args[0]){
			// /holo create KEY
			case "create":
				if(args.length < 2) {
					sendHelpMenu(p);
				} else {

					if(holoKeys.contains(args[1])) {
						Util.coloredMessage(p, "&4[!] &cThe key " + args[1] + " already exist!");
					} else {
						createNewHolo(p.getLocation(), args[1]);
						Util.coloredMessage(p, "&2[!] &aCreated new Hologram with key " + args[1] + " at your location!");
					}
				}
				return true;

			case "tp":
			case "teleport":
				if(args.length < 2) {
					sendHelpMenu(p);
				} else {

					if(holoKeys.contains(args[1])) {
						Util.coloredMessage(p, "&4[!] &cTeleported to " + args[1] + " location");

						p.teleportAsync(getLocFromConfig(args[1]));

					} else {
						Util.coloredMessage(p, "&c[!] Holo" + args[1] + " does not exsist!");
					}
				}
				return true;


			case "delete":
			case "remove":
				if(args.length < 2) {
					sendHelpMenu(p);
				} else {
					if(!holoKeys.contains(args[1])) {
						Util.coloredMessage(p, "&4[!] &cThe key " + args[1] + " does not exsist!");
					} else {
						removeSingleKey(args[1]);
						Util.coloredMessage(p, "&2[!] &aSuccessfully removed " + args[1]);
					}
				}
				return true;
			case "hide":
				if(args.length < 2) {
					sendHelpMenu(p);
				} else {
					if(!holoKeys.contains(args[1])) {
						Util.coloredMessage(p, "&4[!] &cThe key " + args[1] + " does not exsist!");
					} else {
						hideHolo(args[1]);
						Util.coloredMessage(p, "&2[!] &aSuccessfully hid " + args[1]);
					}
				}
				return true;
			case "show":
				if(args.length < 2) {
					sendHelpMenu(p);
				} else {
					if(!holoKeys.contains(args[1])) {
						Util.coloredMessage(p, "&4[!] &cThe key " + args[1] + " does not exsist!");
					} else {
						spawnHolo(args[1]);
						Util.coloredMessage(p, "&2[!] &aSuccessfully shown " + args[1]);
					}
				}
				return true;
			case "list":
				Util.coloredMessage(p, "\n&e&lSERVERTOOLS HOLOGRAMS\n ");
				for(String key : holoKeys) {
					Util.coloredMessage(p, "&b&l"+key+"&7: &f" + HoloConfig.getString(key+".location"));
				}
				return true;

			case "removenear":
				Schedulers.region(plugin, p.getLocation(), () -> {
					int removed = 0;
					for(Entity e : p.getNearbyEntities(2, 2, 2)) {
						if((e instanceof TextDisplay || e instanceof ArmorStand) && e.getLocation().distance(p.getLocation()) <= 2) {
							e.remove();
							removed++;
						}
					}
					Util.coloredMessage(p, "\n&aRemoved " + removed + " armour stand(s) within a radius of 2\n ");
				});
				return true;

			case "reload":
				Util.coloredMessage(p, "&aServertools holograms reloaded!");
				loadInit();
				return true;
			default:
				sendHelpMenu(p);
				return true;
		}
	}

	public void sendHelpMenu(Player p) {
		String[] MSG = new String[] {
			"&7create <name>", "&7remove <name>", "&7show/hide <name>", "&7teleport <name>","&7list",
			"&7removenear", "&7reload",
		};

		Util.coloredMessage(p, "&e&lServerTools Holograms");
		for (int i = 0; i < MSG.length; i++) {
			Util.coloredMessage(p, "&f/hologram " + MSG[i]);
		}

	}

	public void spawnHolo(String key) {
		// TextDisplay renders text at the entity location itself, unlike an ArmorStand
		// nameplate which floated ~2 blocks above the stand.
		// LEGACY: saved Y values in Holograms.yml were authored against the old
		// ArmorStand offset, so subtract 2 to keep existing holos where they were.
		Location base = getLocFromConfig(key).clone().subtract(0, 2, 0);
		World world = base.getWorld();
		//Util.consoleMSG("Loading in hologram: " + key);

		Schedulers.region(plugin, base, () -> {
			Location loc = base;
			TextDisplay td;

			for(String line : getLinesFromConfig(key)) {
				loc = loc.clone().subtract(0, 0.25, 0);

				String msg = Util.color(ConfigUtils.replaceVariable(line));
				if(papiSupport && randomPlayer != null) {
					// sets a random online to be the msg.
					// so this adds support for none player specific Placeholders like %server_uptime%
					msg = PlaceholderAPI.setPlaceholders(randomPlayer, msg);
				}


				if(msg.length() > 0) {
					// display entities have no AI/gravity/collision and aren't ticked like
					// a mob - far cheaper than an ArmorStand nameplate
					td = (TextDisplay) world.spawnEntity(loc, EntityType.TEXT_DISPLAY);
					EntitiyIDs.put(loc, td.getEntityId());
					// legacy '&'/hex codes come through Util.color as section-sign text
					td.text(LegacyComponentSerializer.legacySection().deserialize(msg));
					td.setBillboard(Display.Billboard.CENTER); // always face the player, like a nameplate
				}
			}
		});

	}



	public void createNewHolo(Location l, String newKey) {
		// LEGACY: spawnHolo subtracts 2 from the saved Y at render time (old ArmorStand
		// offset), so store Y+2 here to keep the convention and land the holo where the
		// player is standing. Value is stable across restarts since -2 is always applied.
		l = l.clone().add(0, 2, 0);
		HoloConfig.set(newKey+".location", locationToStringFormat(l));
		HoloConfig.set(newKey+".lines", Arrays.asList("&fEdit this line in", "&bthe Holograms.yml"));
		configUtils.saveConfig(HoloConfig, "Holograms.yml");
		spawnHolo(newKey);
		holoKeys.add(newKey);
	}

	public String locationToStringFormat(Location l) {
		return l.getWorld().getName()+", "+df.format(l.getX())+", "+df.format(l.getY())+", "+df.format(l.getZ());
	}

	public Location getLocFromConfig(String key) {
		String[] loc = HoloConfig.getString(key+".location").split(", ");
		World w = Bukkit.getWorld(loc[0]);
		if(w != null) {
			return new Location(w,
					Double.valueOf(loc[1]),
					Double.valueOf(loc[2]),
					Double.valueOf(loc[3]));
		}
		return null;
	}
	public List<String> getLinesFromConfig(String key) {
		return HoloConfig.getStringList(key+".lines");
	}


	public void removeAllStands(){
		// clear based on config locations (not the in-memory map) so persisted
		// stands from a previous run get removed too, preventing double-spawns on reload
		for(String key : holoKeys) {
			Location l = getLocFromConfig(key);
			if(l == null) {
				continue;
			}
			Schedulers.region(plugin, l, () -> {
				l.getChunk(); // force-load the chunk so persisted stands are found
				// x/z radius must be non-zero: marker stands have a zero-width box and a
				// zero-width search box never overlaps it, so they'd never get removed
				for(Entity e : l.getWorld().getNearbyEntities(l, 2, 8, 2)) {
					// TextDisplay = current holos; ArmorStand w/ visible name = legacy holos
					// from before the TextDisplay migration, cleaned up here on (re)load
					if(e instanceof TextDisplay || (e instanceof ArmorStand && e.isCustomNameVisible())) {
						//Util.consoleMSG("Removed " + e.getCustomName() + " ID:" + e.getEntityId());
						e.remove();
					}
				}
			});
		}
		EntitiyIDs.clear();
	}

	public void removeSingleKey(String key) {
		hideHolo(key);
		HoloConfig.set(key, null);
		holoKeys.remove(key);
		configUtils.saveConfig(HoloConfig, "Holograms.yml");
	}

	public void hideHolo(String key) {
		// removes armour stand from view in the server
		Location l = getLocFromConfig(key);
		// non-zero x/z radius: marker stands have a zero-width box that a zero-width
		// search box never overlaps, so they'd never get removed
		Schedulers.region(plugin, l, () -> {
			for(Entity e : l.getWorld().getNearbyEntities(l, 2, 8, 2)) {
				if(e instanceof TextDisplay) {
					e.remove();
				} else if(e instanceof ArmorStand && e.isCustomNameVisible()) {
					// legacy holo from before the TextDisplay migration
					e.remove();
				}
			}
		});
	}


	public void spawnAllHolos() {

		for(String key : holoKeys) {
			try {
				spawnHolo(key);
			} catch (Exception e) {
				 Util.consoleMSG("&c[!] Could not load Hologram " + key);
			}

    	}
	}

	public void spawnHoloItem(Location l, ItemStack item) {
		// spawn new armour stand at location, but bring back some
		// and try to center more

		Location spawnLoc = l.clone().subtract(0, 2, 0);
//		ArmorStand as = l.getWorld().spawn(l, ArmorStand.class);
//		EntitiyIDs.put(l, as.getEntityId());
//		as.setGravity(false);
//		as.setCanPickupItems(false);
//		as.setCustomNameVisible(false);
//		as.setVisible(false);
//		as.setItemInHand(item);
//		as.setRightArmPose(new EulerAngle(-3.25, -0, -2.5));
//		EntitiyIDs.put(l, as.getEntityId());
		Schedulers.region(plugin, spawnLoc, () -> {
			ArmorStand as = spawnLoc.getWorld().spawn(spawnLoc, ArmorStand.class);
			EntitiyIDs.put(spawnLoc, as.getEntityId());
			as.setGravity(false);
			as.setCanPickupItems(false);
			as.setVisible(false);
			Entity e = spawnLoc.getWorld().dropItem(spawnLoc, item);
			as.setPassenger(e);
		});

	}
}
