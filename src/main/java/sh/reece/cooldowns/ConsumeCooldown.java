package sh.reece.cooldowns;

import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import sh.reece.tools.Main;
import sh.reece.tools.ToggleableListener;
import sh.reece.utiltools.Util;

public abstract class ConsumeCooldown extends ToggleableListener {

    private final int cooldownSeconds;
    private final String cooldownMessage;
    private final String eatenMessage;
    private final HashMap<String, Long> cooldownMap = new HashMap<>();
    private final Material material;
    private final short durability;

    protected ConsumeCooldown(Main plugin, String section, Material material, short durability) {
        super(plugin, section);
        this.material = material;
        this.durability = durability;
        if (isEnabled()) {
            this.cooldownSeconds = plugin.getConfig().getInt(section + ".Seconds");
            this.cooldownMessage = plugin.getConfig().getString(section + ".Message");
            String msg = plugin.getConfig().getString(section + ".StartCooldownMSG");
            this.eatenMessage = msg != null ? msg.replace("%seconds%", String.valueOf(cooldownSeconds)) : "";
        } else {
            this.cooldownSeconds = 0;
            this.cooldownMessage = null;
            this.eatenMessage = null;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent e) {
        Player p = e.getPlayer();
        ItemStack item = e.getItem();
        if (item.getType() == material && item.getDurability() == durability) {
            if (!Util.cooldown(cooldownMap, cooldownSeconds, p.getName(), cooldownMessage)) {
                e.setCancelled(true);
            } else if (eatenMessage != null && !eatenMessage.isEmpty()) {
                Util.coloredMessage(p, eatenMessage);
            }
        }
    }
}
