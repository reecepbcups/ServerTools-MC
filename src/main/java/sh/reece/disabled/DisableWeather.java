package sh.reece.disabled;

import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.weather.WeatherChangeEvent;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;

public class DisableWeather extends ToggleableListener {

	private List<String> worlds;

	public DisableWeather(Main instance) {
		super(instance, "Disabled.DisableWeather");

		if (isEnabled()) {
			worlds = plugin.getConfig().getStringList("Disabled.DisableWeather.worlds");
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
