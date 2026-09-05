package sh.reece.core.warp.v2;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public record WarpConfiguration(String deleteWarp, String setWarp, String viewWarps, String warpOthers) {

    public static final String SECTION = "Core.Warps";

    public boolean canDeleteWarp(Player player) {
        return canDo(this.deleteWarp, player);
    }

    public boolean canSetWarp(Player player) {
        return canDo(this.setWarp, player);
    }

    public boolean canViewWarps(Player player) {
        return canDo(this.viewWarps, player);
    }

    public boolean canWarpOthers(Player player) {
        return canDo(this.warpOthers, player);
    }

    private static boolean canDo(String permission, Player player) {
        return permission.isBlank() || player.hasPermission(permission);
    }

    public static WarpConfiguration fromConfig(FileConfiguration configuration) {
        ConfigurationSection section = configuration.getConfigurationSection(SECTION);
        return new WarpConfiguration(
                section.getString( "DeleteWarpPerm", ""),
                section.getString("SetWarpPerm", ""),
                section.getString("ViewWarpPerm", ""),
                section.getString("WarpOtherPlayToWarpPerm", "")
        );
    }
}
