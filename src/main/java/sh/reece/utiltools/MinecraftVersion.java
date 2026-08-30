package sh.reece.utiltools;

import org.bukkit.Bukkit;

public enum MinecraftVersion {         
    V1_8_R1, 
    V1_8_R2, 
    V1_8_R3, 
    V1_9_R1, 
    V1_9_R2,
    V1_10_R1, 
    V1_11_R1, 
    V1_12_R1, 
    V1_13_R1, 
    V1_13_R2, 
    V1_14_R1, 
    V1_15_R1, 
    V1_16_R1, 
    V1_16_R2, 
    V1_16_R3, 
    V1_17_R1, // we don't have 1.18+ here, bc it throws UNKNOWN. This means it is 1.18+
    SUPPORTED;

    public static MinecraftVersion getVersion() { // pretty? no but gets the job done
        MinecraftVersion mv = null;
        // Paper 1.20.5+ dropped the versioned package (org.bukkit.craftbukkit, no v1_XX_RX)
        String[] parts = Bukkit.getServer().getClass().getPackage().getName().split("\\.");
        if (parts.length < 4) {
            return MinecraftVersion.SUPPORTED;
        }
        String ver = parts[3].toUpperCase();
        try {
            mv = MinecraftVersion.valueOf(ver);
        } catch (Exception e) {
            // Util.log("Version: " + ver + " not found in enum constants" + e.getMessage());
            mv = MinecraftVersion.SUPPORTED;
        }
        return mv;
    }

    public boolean isAbove(MinecraftVersion compare) {        
        return ordinal() > compare.ordinal();
    }

    public boolean isAboveOrEqual(MinecraftVersion compare) {
        return (ordinal() >= compare.ordinal());
    }

    public boolean isBelow(MinecraftVersion compare){
        return (ordinal() < compare.ordinal());
    }

    public boolean isBelowOrEqual(MinecraftVersion compare){
        return (ordinal() <= compare.ordinal());
    }

    public boolean isEqual(MinecraftVersion compare){
        return (ordinal() == compare.ordinal());
    }
}
