package sh.reece.tools;

import org.bukkit.Bukkit;

import sh.reece.GUI.*;
import sh.reece.bungee.*;
import sh.reece.chat.*;
import sh.reece.cmds.*;
import sh.reece.cooldowns.*;
import sh.reece.core.*;
import sh.reece.core.warp.WarpCMD;
import sh.reece.disabled.*;
import sh.reece.events.*;
import sh.reece.moderation.*;
import sh.reece.runnables.*;
import sh.reece.utiltools.MinecraftVersion;
import sh.reece.utiltools.Util;

public class Loader {

	private Main plugin;
	private Timings executionTimer;

	private Enderchest reeceEnder;
	private InvSee reeceInvSee;
	private ChatColor chatcolor;
	private Holograms holograms;
	private DailyRewards dailyrewards;

	public Loader(Main instance) {
		plugin = instance;


		if(MinecraftVersion.getVersion() != MinecraftVersion.SUPPORTED) {
			Util.log("\n\n\n[ServerTools] &cYou are running an unsupported version of Minecraft for this version of the plugin.");
			Util.log("[ServerTools] &eYou can either update to 1.18.x &fOR &euse this version:");
			Util.log("[ServerTools] &ehttps://www.spigotmc.org/resources/servertools-%E2%9E%9C-modular-server-management-1-8-1-18-2-open-source.95853/download?version=455997");
			Util.log("[ServerTools] &cSource Here:");
			Util.log("[ServerTools] &ehttps://github.com/Reecepbcups/ServerTools-MC/tree/ALL_1.8-%3E1.18");

			Util.coloredBroadcast("[ServerTools] This version only supports 1.18+! To use for <=1.17, download here:");
			Util.coloredBroadcast("[ServerTools] https://www.spigotmc.org/resources/servertools-%E2%9E%9C-modular-server-management-1-8-1-18-2-open-source.95853/download?version=455997");
			Util.coloredBroadcast("[ServerTools] Source Here:");
			Util.coloredBroadcast("[ServerTools] &ehttps://github.com/Reecepbcups/ServerTools-MC/tree/ALL_1.8-%3E1.18");

			plugin.getServer().getPluginManager().disablePlugin(plugin);
			return;
		}

		executionTimer = new Timings();
		executionTimer.start();
	}

	public void loadPlaceholderAPI() {
		plugin.setPAPIStatus(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"));
		if (plugin.isPAPIEnabled()) {
			(new ServerToolsPlaceholders()).register();
			executionTimer.info("PAPI");
		}
	}

	public void loadCommands() {
		// COMMANDS
		new AltTP(plugin);
		new ChangeSlots(plugin);
		new ChatPoll(plugin);
		new CommandSpy(plugin);
		dailyrewards = new DailyRewards(plugin);
		new Donation(plugin);
		new FancyAnnounce(plugin);
		new Rename(plugin);
		new ServerInfoCMDS(plugin);
		new TPAll(plugin);
		new Visibility(plugin);
		new Countdown(plugin);
		new Reclaim(plugin);
		new ClearLag(plugin);
		new GiveAll(plugin);
		new StaffList(plugin);
		new Speed(plugin);
		new BungeeServerConnector(plugin);
		new ReeceTools(plugin);
		new ItemDB(plugin);
		executionTimer.info("Commands");
	}

	public void loadCore() {
		// Core (Essentials Clone)
		// https://github.com/EssentialsX/Essentials/tree/2.x/Essentials/src/main/java/com/earth2me/essentials/commands
		new Fly(plugin);
		new TP(plugin);
		new Heal(plugin);
		new Gamemode(plugin);
		new ClearInv(plugin);
		new Broadcast(plugin);
		new AdminChat(plugin);
		new Workbench(plugin);
		new Compass(plugin);
		new Messaging(plugin);
		new Nickname(plugin);
		new Trash(plugin);
		new Top(plugin);
		new God(plugin);
		new Ping(plugin);
		new Repair(plugin);
		reeceEnder = new Enderchest(plugin);
		reeceInvSee = new InvSee(plugin);
		new Hat(plugin);
		new Extinguish(plugin);
		// new Enchant(plugin);
		new WarpCMD(plugin);

		executionTimer.info("Core Features");
	}

	public void loadEvents() {
		new AntiCraft(plugin);
		new CMDAlias(plugin);
		chatcolor = new ChatColor(plugin);
		new ChatCooldown(plugin);
		new ChatEmotes(plugin);
		new ChatFormat(plugin);
		new ChatNumberGuesser(plugin);
		new ColonInCommands(plugin);
		new CustomDeathMessages(plugin);
		new OnJoinCommands(plugin);
		new JoinMOTD(plugin);
		new NoBedExplosion(plugin);
		new ShopClickWorkAround(plugin);
		new Spawn(plugin);
		new StackUnstackables(plugin);
		new WhitelistBypass(plugin);
		new WorldEffects(plugin);
		new DisableGolemPoppies(plugin);
		new LaunchPads(plugin);
		new ThreeHitGlitch(plugin);
		new DisableJLMsg(plugin);
		new DisableStackablePotions(plugin);

		executionTimer.info("Events");
	}

	public void loadVaultDependentPlugins() {
		// If vault is installed these will be allowed
		if (Util.isPluginInstalledOnServer("vault", "Withdraw")) {
			new Tags(plugin);
			new Withdraw(plugin);
			new XPBottle(plugin);
			executionTimer.info("Vault Required");
		} else {
			Util.consoleMSG("&eVault not installed. Tags, Withdraw, and XPBottle can not be enabled.");
		}
	}

	public void loadCooldowns() {
		new EnderPearlCooldown(plugin);
		new GodAppleCooldown(plugin);
		new GoldenAppleCooldown(plugin);
		executionTimer.info("Cooldowns");
	}

	public void loadToggleableFeatures() {
		new BlockBreaking(plugin);
		new BlockPlacement(plugin);
		new BlazeDrowning(plugin);

		new DisableBookWriting(plugin);
		new DisableCactusDamage(plugin);
		new DisableCaneOnCane(plugin);
		new DisableCropTrample(plugin);
		new DisableDisconnectSpam(plugin);
		new DisableDragonEggTP(plugin);
		new DisableEndermanTP(plugin);
		new DisableFallDamage(plugin);
		new DisableGrassDecay(plugin);
		new DisableHunger(plugin);
		new DisableItemBurn(plugin);
		new DisableJockeys(plugin);
		new DisableLeaveDecay(plugin);
		new DisableMobAI(plugin);
		new DisableThowingItems(plugin);
		new DisableVillagerTrading(plugin);
		new DisableWaterBreakingRedstone(plugin);
		new DisableWeather(plugin);
		new DisableWitherBreak(plugin);
		new DisableMobSpawning(plugin);
		new DisableWorldGuardGlitchBuilding(plugin);
		new DisablePhantomSpawn(plugin);
		new DisableIceMelt(plugin);
		executionTimer.info("Toggleable");
	}

	public void loadModeration() {
		new ClearChat(plugin);
		new CommandProtection(plugin);
		new Freeze(plugin);
		new MuteChat(plugin);
		new StaffAFK(plugin);
		new Report(plugin);
		executionTimer.info("Moderation");
	}

	public void loadGUIs() {
		new FeaturesGUI(plugin);
		new ShopClickWorkAround(plugin);
		new NameColor(plugin);
		new Vouchers(plugin);
		executionTimer.info("GUIs");
	}

	public void loadRunnableTask() {
		new AutoBroadcast(plugin);
		new TimeChange(plugin);
		new ScheduledTask(plugin);
		holograms = new Holograms(plugin);
		executionTimer.info("Runnables & Holograms");
	}

	// -= ACTIONS =-
	public void output() {
		String ver = plugin.getDescription().getVersion();
		Util.consoleMSG("\n&b&l[!] ServerTools&b by reecepbcups. Version: " + ver);
		if (plugin.getConfig().getBoolean("LoadWithTimings")) {
			Main.logging(executionTimer.end());
		}
	}

	public void setMarking(String mark) {
		executionTimer.info(mark);
	}

	public void unloadAll() {
		plugin.saveDefaultConfig();
		plugin.modulesList.clear();
		Bukkit.getServer().getScheduler().cancelTasks(plugin);
		ChangeSlots.saveNewChangeSlotsPlayers();
		dailyrewards.saveCooldownsToFile();
		if(chatcolor != null) {
			chatcolor.saveChatColorToFile();
		}
		if(holograms != null) {
			holograms.removeAllStands();
		}
		LaunchPads.stopLaunchpadChecking();

		reeceEnder.closeAllViewedEnderchest();
		reeceInvSee.closeAllViewedInvsee();
	}

}
