package sh.reece.runnables;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;

import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class TimeChange {

	public TimeChange(Main instance) {
		String Section = "Disabled.DisableTimeChange";
		if (instance.getConfigUtils().enabledInConfig(Section + ".Enabled")) {
			List<World> dayWorlds = getNonNullWorlds(instance.getConfig().getStringList(Section + ".DayWorlds"));
			List<World> nightWorlds = getNonNullWorlds(instance.getConfig().getStringList(Section + ".NightWorlds"));

			for (World w : dayWorlds) {
				w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
				w.setTime(4000);
			}
			for (World w : nightWorlds) {
				w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
				w.setTime(16000);
			}
		}
	}

	private List<World> getNonNullWorlds(List<String> worlds) {
		List<World> worldList = new ArrayList<>();
		for (String world : worlds) {
			World w = Bukkit.getWorld(world);
			if (w != null) {
				worldList.add(w);
			} else {
				Util.consoleMSG("&cWorld: " + world + " is not a world!");
			}
		}
		return worldList;
	}
}
