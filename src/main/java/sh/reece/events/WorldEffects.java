package sh.reece.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

	// pre-resolved at init: WorldName -> [(PotionEffectType, level)]
	private final Map<String, List<ResolvedEffect>> worldEffects = new HashMap<>();

	private final HashMap<UUID, List<String>> affectedPlayers = new HashMap<>();

	private record ResolvedEffect(PotionEffectType type, int level) {}

	public WorldEffects(Main instance) {
		super(instance, "Events.WorldEffects");

		if (isEnabled()) {
			String Section = "Events.WorldEffects";
			for (String wEff : instance.getConfig().getStringList(Section + ".worlds")) {
				String[] wEffSplit = wEff.split(":");

				int value = 1;
				if (wEffSplit.length > 2) {
					try {
						value = Integer.parseInt(wEffSplit[2]);
					} catch (NumberFormatException e) {
						Util.log(wEffSplit[2] + " is not a valid number");
					}
				}

				PotionEffectType type = PotionEffectType.getByName(wEffSplit[1].toUpperCase());
				if (type == null) {
					Util.log("Unknown potion effect: " + wEffSplit[1]);
					continue;
				}

				worldEffects.computeIfAbsent(wEffSplit[0], k -> new ArrayList<>())
					.add(new ResolvedEffect(type, value));
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

		List<ResolvedEffect> effects = worldEffects.get(w);
		if (effects == null) return;

		for (ResolvedEffect eff : effects) {
			p.addPotionEffect(new PotionEffect(eff.type, Integer.MAX_VALUE, eff.level));
		}

		affectedPlayers.computeIfAbsent(p.getUniqueId(), k -> new ArrayList<>()).add(w);
	}

	public void removeEffect(Player p) {
		List<String> worlds = affectedPlayers.get(p.getUniqueId());

		if (worlds != null) {
			for (String worldName : worlds) {
				List<ResolvedEffect> effects = worldEffects.get(worldName);
				if (effects == null) continue;

				for (ResolvedEffect eff : effects) {
					p.removePotionEffect(eff.type);
				}
			}

			affectedPlayers.remove(p.getUniqueId());
		}
	}
}
