package sh.reece.core.kit;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Banner;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.reece.utiltools.Util;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Turns kit item lines ({@code dpickaxe 1 efficiency:5 name:&bDrill} or {@code @<base64>}) into items. */
final class KitItems {

    private static final Logger LOGGER = LoggerFactory.getLogger(KitItems.class);

    private static final Pattern SPACES = Pattern.compile(" +");
    private static final Pattern UNESCAPED_UNDERSCORE = Pattern.compile("(?<!\\\\)_");
    private static final Pattern UNESCAPED_PIPE = Pattern.compile("(?<!\\\\)\\|");
    private static final Pattern VALID_KEY = Pattern.compile("[a-z0-9_.\\-/]+");

    // old Bukkit field names (pre 1.20.5) that existing kit files still use
    private static final Map<String, String> LEGACY_ENCHANTS = Map.ofEntries(
            Map.entry("protection_environmental", "protection"),
            Map.entry("protection_fire", "fire_protection"),
            Map.entry("protection_fall", "feather_falling"),
            Map.entry("protection_explosions", "blast_protection"),
            Map.entry("protection_projectile", "projectile_protection"),
            Map.entry("water_worker", "aqua_affinity"),
            Map.entry("damage_all", "sharpness"),
            Map.entry("damage_undead", "smite"),
            Map.entry("damage_arthropods", "bane_of_arthropods"),
            Map.entry("loot_bonus_mobs", "looting"),
            Map.entry("loot_bonus_blocks", "fortune"),
            Map.entry("dig_speed", "efficiency"),
            Map.entry("arrow_damage", "power"),
            Map.entry("arrow_knockback", "punch"),
            Map.entry("arrow_fire", "flame"),
            Map.entry("arrow_infinite", "infinity"),
            Map.entry("luck", "luck_of_the_sea"));

    private KitItems() {}

    /** @throws IllegalArgumentException when the item or one of its enchants can't be resolved */
    static ItemStack parse(final String line, final boolean allowUnsafe) {
        if (line.startsWith("@")) {
            return ItemStack.deserializeBytes(Base64.getMimeDecoder().decode(line.substring(1).trim()));
        }

        final String[] parts = SPACES.split(line.trim());
        final int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
        ItemStack stack = base(parts[0], amount);

        final PendingPotion potion = new PendingPotion();
        for (int i = 2; i < parts.length; i++) {
            stack = applyMeta(stack, parts[i], allowUnsafe, potion, line);
        }
        return stack;
    }

    static String serialize(final ItemStack item) {
        return "@" + Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    /** {@code dpickaxe}, {@code diamond_pickaxe}, {@code minecraft:diamond_pickaxe} or {@code daxe:780} (damage). */
    private static ItemStack base(String name, final int amount) {
        int damage = 0;
        final int colon = name.lastIndexOf(':');
        if (colon > 0 && Util.isInt(name.substring(colon + 1))) {
            damage = Integer.parseInt(name.substring(colon + 1));
            name = name.substring(0, colon);
        }

        String subtype = null;
        Material material = Material.matchMaterial(name);
        if (material == null) {
            final String alias = KitAliases.get().item(name);
            if (alias == null) {
                throw new IllegalArgumentException("unknown item " + name);
            }
            final int split = alias.indexOf(':');
            material = Material.matchMaterial(split < 0 ? alias : alias.substring(0, split));
            subtype = split < 0 ? null : alias.substring(split + 1);
            if (material == null) {
                throw new IllegalArgumentException("item " + name + " doesn't exist on this server version");
            }
        }

        final ItemStack stack = new ItemStack(material, amount);
        final ItemMeta meta = stack.getItemMeta();
        if (damage > 0 && meta instanceof Damageable damageable) {
            damageable.setDamage(damage);
        }
        if (subtype != null) {
            applySubtype(meta, subtype);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private static void applySubtype(final ItemMeta meta, final String subtype) {
        try {
            if (meta instanceof PotionMeta potionMeta) {
                potionMeta.setBasePotionType(PotionType.valueOf(subtype));
            } else if (meta instanceof BlockStateMeta stateMeta && stateMeta.getBlockState() instanceof CreatureSpawner spawner) {
                spawner.setSpawnedType(EntityType.valueOf(subtype));
                stateMeta.setBlockState(spawner);
            }
        } catch (final IllegalArgumentException e) {
            // subtype from a newer or older version than this server, plain item is close enough
        }
    }

    private static ItemStack applyMeta(ItemStack stack, final String token, final boolean allowUnsafe,
                                       final PendingPotion potion, final String line) {
        final String[] split = token.split(":", 2);
        final String key = split[0].toLowerCase(Locale.ROOT);
        final String value = split.length > 1 ? split[1] : null;
        final ItemMeta meta = stack.getItemMeta();

        if (value != null && key.equals("name")) {
            meta.setDisplayName(Util.color(text(value)));
        } else if (value != null && (key.equals("lore") || key.equals("desc"))) {
            final List<String> lore = new ArrayList<>();
            for (final String part : UNESCAPED_PIPE.split(value)) {
                lore.add(Util.color(text(part).replace("\\|", "|")));
            }
            meta.setLore(lore);
        } else if (key.equals("custom-model-data") || key.equals("cmd")) {
            meta.setCustomModelData(value == null ? 0 : Integer.parseInt(value));
        } else if (key.equals("unbreakable")) {
            meta.setUnbreakable(value == null || Boolean.parseBoolean(value));
        } else if (value != null && (key.equals("player") || key.equals("owner")) && meta instanceof SkullMeta skull) {
            // name only profile, the server fills in the skin later so this never blocks on a lookup
            skull.setPlayerProfile(Bukkit.createProfile(value));
        } else if (value != null && (key.equals("color") || key.equals("colour")) && meta instanceof LeatherArmorMeta leather) {
            leather.setColor(color(value));
        } else if (value != null && key.equals("basecolor") && meta instanceof BlockStateMeta stateMeta
                && stateMeta.getBlockState() instanceof Banner banner) {
            // shields: the rgb has to land on one of the 16 dye colors
            final DyeColor dye = DyeColor.getByColor(color(value));
            if (dye == null) {
                throw new IllegalArgumentException("basecolor " + value + " isn't a dye color");
            }
            banner.setBaseColor(dye);
            stateMeta.setBlockState(banner);
        } else if (value != null && key.equals("itemflags")) {
            for (final String flag : value.split(",")) {
                meta.addItemFlags(ItemFlag.valueOf(flag.trim().toUpperCase(Locale.ROOT)));
            }
        } else if (value != null && key.equals("title") && meta instanceof BookMeta book) {
            book.setTitle(Util.color(text(value)));
        } else if (value != null && key.equals("author") && meta instanceof BookMeta book) {
            book.setAuthor(Util.color(value));
        } else if (value != null && key.equals("power") && meta instanceof FireworkMeta firework) {
            final int power = Util.isInt(value) ? Integer.parseInt(value) : 0;
            firework.setPower(power > 3 ? 4 : power);
        } else if (value != null && meta instanceof PotionMeta potionMeta && potion.accepts(key)) {
            potion.set(key, value);
            if (potion.splash && stack.getType() == Material.POTION) {
                stack = stack.withType(Material.SPLASH_POTION);
                return applyMeta(stack, token, allowUnsafe, potion, line);
            }
            if (potion.ready()) {
                potionMeta.addCustomEffect(potion.build(), true);
                potion.reset();
            }
        } else if (!enchant(meta, key, value, allowUnsafe, stack)) {
            LOGGER.warn("Ignoring unsupported kit item meta '{}' in: {}", token, line);
            return stack;
        }

        stack.setItemMeta(meta);
        return stack;
    }

    /** @return false when the key isn't an enchant at all */
    private static boolean enchant(final ItemMeta meta, final String key, final String value,
                                   final boolean allowUnsafe, final ItemStack stack) {
        Enchantment enchantment = VALID_KEY.matcher(key).matches()
                ? Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key)) : null;
        if (enchantment == null) {
            String alias = KitAliases.get().enchant(key);
            if (alias == null) {
                alias = LEGACY_ENCHANTS.get(key);
            }
            enchantment = alias == null ? null : Registry.ENCHANTMENT.get(NamespacedKey.minecraft(alias));
        }
        if (enchantment == null) {
            return false;
        }

        final int level = value != null && Util.isInt(value) ? Integer.parseInt(value) : enchantment.getMaxLevel();
        if (level <= 0) {
            return true;
        }
        final boolean book = meta instanceof EnchantmentStorageMeta;
        if (!allowUnsafe && (level > enchantment.getMaxLevel() || (!book && !enchantment.canEnchantItem(stack)))) {
            throw new IllegalArgumentException("unsafe enchant " + key + ":" + level + " on " + stack.getType()
                    + " (set AllowUnsafeEnchantments to allow it)");
        }

        if (book) {
            ((EnchantmentStorageMeta) meta).addStoredEnchant(enchantment, level, true);
        } else {
            meta.addEnchant(enchantment, level, true);
        }
        return true;
    }

    /** {@code #ff0000}, a packed int, or {@code r,g,b}. */
    private static Color color(final String value) {
        final String[] rgb = value.split("[|,]");
        if (rgb.length == 3) {
            return Color.fromRGB(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
        }
        if (value.startsWith("#")) {
            return Color.fromRGB(Integer.parseInt(value.substring(1), 16));
        }
        return Color.fromRGB(Integer.parseInt(value));
    }

    /** _ is a space, \_ is a literal underscore. */
    static String text(final String value) {
        return UNESCAPED_UNDERSCORE.matcher(value).replaceAll(" ").replace("\\_", "_");
    }

    /** Potion effects take effect, power and duration across separate tokens. */
    private static final class PendingPotion {
        private PotionEffectType type;
        private int amplifier = -1;
        private int durationTicks = Integer.MIN_VALUE;
        private boolean splash;

        boolean accepts(final String key) {
            return switch (key) {
                case "effect", "power", "amplifier", "duration", "splash" -> true;
                default -> false;
            };
        }

        void set(final String key, final String value) {
            switch (key) {
                case "effect" -> this.type = effect(value);
                // power is 1 based like the in game numeral, amplifier is the raw value
                case "power" -> {
                    final int power = Integer.parseInt(value);
                    this.amplifier = power > 0 && power < 4 ? power - 1 : power;
                }
                case "amplifier" -> this.amplifier = Integer.parseInt(value);
                case "duration" -> {
                    final int seconds = Integer.parseInt(value);
                    this.durationTicks = seconds == -1 ? -1 : seconds * 20;
                }
                case "splash" -> this.splash = Boolean.parseBoolean(value);
                default -> { }
            }
        }

        boolean ready() {
            return this.type != null && this.amplifier >= 0 && this.durationTicks != Integer.MIN_VALUE;
        }

        PotionEffect build() {
            return new PotionEffect(this.type, this.durationTicks, this.amplifier);
        }

        void reset() {
            this.type = null;
            this.amplifier = -1;
            this.durationTicks = Integer.MIN_VALUE;
        }

        private static PotionEffectType effect(final String name) {
            final String lower = name.toLowerCase(Locale.ROOT);
            PotionEffectType type = VALID_KEY.matcher(lower).matches()
                    ? Registry.EFFECT.get(NamespacedKey.minecraft(lower)) : null;
            if (type == null) {
                final String alias = KitAliases.get().effect(lower);
                type = alias == null ? null : Registry.EFFECT.get(NamespacedKey.minecraft(alias));
            }
            if (type == null) {
                throw new IllegalArgumentException("unknown potion effect " + name);
            }
            return type;
        }
    }
}
