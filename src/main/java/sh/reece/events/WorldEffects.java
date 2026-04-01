package sh.reece.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;
import sh.reece.utiltools.Util;

public class WorldEffects extends ToggleableListener {

	// WorldName, <PotionEffectName, LevelEffect>
	private Map<String, Map<String, Integer>> world_effect = new HashMap<>();

	// USER, World
	private HashMap<UUID, List<String>> affectedPlayers = new HashMap<>();

	public WorldEffects(Main instance) {
		super(instance, "Events.WorldEffects");

		if (isEnabled()) {
			String Section = "Events.WorldEffects";
			for (String wEff : instance.getConfig().getStringList(Section + ".worlds")) {
				String[] wEffSplit = wEff.split(":");

				int value = 1;
				if (wEffSplit.length > 2) {
					try {
						value = Integer.valueOf(wEffSplit[2]);
					} catch (Exception e) {
						Util.log(wEffSplit[2] + " is not a valid number");
					}
				}

				Map<String, Integer> eff = world_effect.get(wEffSplit[0]);
				if (eff == null) {
					eff = new HashMap<>();
				}

				eff.put(wEffSplit[1], value);
				world_effect.put(wEffSplit[0], eff);
				Util.log("WorldEffect: " + wEffSplit[0] + " " + wEffSplit[1] + " " + value);
			}

			if (world_effect == null) {
				Util.log("No world effects found!!");
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void playerChangeWorldEvent(PlayerChangedWorldEvent e) {
		addEffect(e.getPlayer());
	}

	@EventHandler(ignoreCancelled = true)
	public void onJoin(PlayerJoinEvent e) {
		addEffect(e.getPlayer());
	}

	@EventHandler(ignoreCancelled = true)
	public void onLeave(PlayerQuitEvent e) {
		removeEffect(e.getPlayer());
	}

	@EventHandler(ignoreCancelled = true)
	public void onKick(PlayerKickEvent e) {
		removeEffect(e.getPlayer());
	}

	public void addEffect(Player p) {
		String w = p.getWorld().getName();

		removeEffect(p);

		if (world_effect.containsKey(w)) {
			Map<String, Integer> potionEffects = world_effect.get(w);

			for (String potionkey : potionEffects.keySet()) {
				int value = potionEffects.get(potionkey);

				PotionEffectType potion = PotionEffectType.getByName(potionkey.toUpperCase());
				p.addPotionEffect(new PotionEffect(potion, Integer.MAX_VALUE, value));

				Util.log("Added: " + potionkey + " to " + p.getName());
			}

			List<String> worldsEffects = affectedPlayers.get(p.getUniqueId());
			if (worldsEffects == null) {
				worldsEffects = new ArrayList<>();
			}
			worldsEffects.add(w);

			affectedPlayers.put(p.getUniqueId(), worldsEffects);
		}
	}

	public void removeEffect(Player p) {
		List<String> worlds = affectedPlayers.get(p.getUniqueId());

		if (worlds != null) {
			for (String worldName : worlds) {
				if (!world_effect.containsKey(worldName)) {
					Util.log("No effects found for world: " + worldName);
					return;
				}

				for (Entry<String, Integer> effects : world_effect.get(worldName).entrySet()) {
					PotionEffectType potion = PotionEffectType.getByName(effects.getKey().toUpperCase());
					p.removePotionEffect(potion);
					Util.log("Removed: " + effects.getKey() + " from " + p.getName());
				}
			}

			affectedPlayers.remove(p.getUniqueId());
		}
	}
}
