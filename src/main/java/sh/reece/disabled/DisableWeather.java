package sh.reece.disabled;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.weather.WeatherChangeEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;

public class DisableWeather extends ToggleableListener {

	private Set<String> worlds;

	public DisableWeather(Main instance) {
		super(instance, "Disabled.DisableWeather");

		if (isEnabled()) {
			worlds = new HashSet<>(plugin.getConfig().getStringList("Disabled.DisableWeather.worlds"));
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	  public void WeatherChangeEvent(WeatherChangeEvent event) {
	    if (!event.toWeatherState())
	      return;
	    if (worlds.contains(event.getWorld().getName())) {
	      event.setCancelled(true);
	      event.getWorld().setWeatherDuration(0);
	      event.getWorld().setThundering(false);
	    }
	  }


}
