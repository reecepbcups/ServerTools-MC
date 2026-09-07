package sh.reece.core.warp;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public record WarpConfiguration(String deleteWarp, String setWarp, String viewWarps, String warpOthers) {

    public static final String SECTION = "Core.Warps";

    private static final String DEFAULT_DELETE_WARP = "tools.delwarp";
    private static final String DEFAULT_SET_WARP = "tools.setwarp";
    private static final String DEFAULT_VIEW_WARPS = "tools.viewwarp";
    private static final String DEFAULT_WARP_OTHERS = "tools.warpother";

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
        if (section == null) {
            return new WarpConfiguration(DEFAULT_DELETE_WARP, DEFAULT_SET_WARP,
                    DEFAULT_VIEW_WARPS, DEFAULT_WARP_OTHERS);
        }

        return new WarpConfiguration(
                node(section, "DeleteWarpPerm", DEFAULT_DELETE_WARP),
                node(section, "SetWarpPerm", DEFAULT_SET_WARP),
                node(section, "ViewWarpPerm", DEFAULT_VIEW_WARPS),
                node(section, "WarpOtherPlayToWarpPerm", DEFAULT_WARP_OTHERS)
        );
    }

    private static String node(ConfigurationSection section, String key, String fallback) {
        String configured = section.getString(key);
        return configured == null || configured.isBlank() ? fallback : configured;
    }
}
