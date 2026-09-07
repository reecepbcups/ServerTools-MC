package sh.reece.core;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Registry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Enchant extends BaseCommand {

    public static final String PLAYER_ONLY_ENCHANT = "<red>[!] Only players can enchant items!";
    public static final String ENCHANT_USAGE = "<red>[!] Usage: <gray>/enchant \\<enchantment> [level]";
    public static final String ENCHANT_INVALID = "<red>[!] <gray><name> <red>is not a valid enchantment";
    public static final String ITEM_IN_HAND = "<red>[!] <gray>You need to hold an item in your hand!";
    public static final String INVALID_INTEGER = "<red>[!] <gray><value> <red>is not a valid integer";
    public static final String LEVEL_OUT_OF_RANGE =
            "<red>[!] <gray>Level must be between <white>%d <gray>and <white>%d<gray>.";
    public static final String ENCHANTED = "<green>[+] <gray>Applied <white><enchantment> <gray>to your <white><item><gray>.";

    // paper enchants are scoped starting at 1 guarenteed, 255 is a logical maximum
    private static final int MIN_LEVEL = 1;
    private static final int MAX_LEVEL = 255;

    public Enchant(Main instance) {
        super(instance, "Core.Enchant", "enchant");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendRichMessage(PLAYER_ONLY_ENCHANT);
            return true;
        }

        if (noPermission(sender, cmd)) return true;

        if (args.length == 0) {
            sender.sendRichMessage(ENCHANT_USAGE);
            return true;
        }

        ItemStack heldItem = p.getInventory().getItem(EquipmentSlot.HAND);
        if (heldItem.isEmpty()) {
            p.sendRichMessage(ITEM_IN_HAND);
            return true;
        }

        Enchantment enchantment = resolveEnchantment(args[0]);
        if (enchantment == null) {
            p.sendRichMessage(ENCHANT_INVALID, Placeholder.unparsed("name", args[0]));
            return true;
        }

        int level = MIN_LEVEL;
        if (args.length >= 2) {
            try {
                level = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                p.sendRichMessage(INVALID_INTEGER, Placeholder.unparsed("value", args[1]));
                return true;
            }

            if (level < MIN_LEVEL || level > MAX_LEVEL) {
                p.sendRichMessage(LEVEL_OUT_OF_RANGE.formatted(MIN_LEVEL, MAX_LEVEL));
                return true;
            }
        }

        heldItem.addUnsafeEnchantment(enchantment, level);
        p.getInventory().setItem(EquipmentSlot.HAND, heldItem);

        p.sendRichMessage(ENCHANTED,
                Placeholder.component("enchantment", enchantmentDisplayName(enchantment, level)),
                Placeholder.component("item", Component.translatable(heldItem)));
        return true;
    }

    /**
     * Like {@link Enchantment#displayName(int)} but renders the level as a plain number instead of
     * the {@code enchantment.level.N} translation, keeping Paper's name and colouring. The in-world
     * item tooltip is rewritten separately by {@link EnchantTooltipListener}.
     */
    private static Component enchantmentDisplayName(final Enchantment enchantment, final int level) {
        final Component full = enchantment.displayName(level);
        if (level == 1 && enchantment.getMaxLevel() == 1) {
            return full; // single-level enchant: vanilla shows no level at all
        }
        return full.children(List.of())
                .append(Component.space())
                .append(Component.text(level));
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command cmd, final String alias, final String[] args) {
        if (!hasPermission(sender, this.permission)) {
            return List.of();
        }

        if (args.length == 1) {
            return suggestEnchantments(args[0]);
        }

        if (args.length == 2) {
            final Enchantment enchantment = resolveEnchantment(args[0]);
            return enchantment == null ? List.of() : suggestLevels(enchantment, args[1]);
        }

        return List.of();
    }

    /**
     * Vanilla-style suggestions: {@code minecraft} entries are offered unqualified until the
     * sender types a {@code :}, after which only fully qualified keys can match.
     */
    private static List<String> suggestEnchantments(final String prefix) {
        final String lower = prefix.toLowerCase(Locale.ROOT);
        final boolean qualified = lower.indexOf(Key.DEFAULT_SEPARATOR) >= 0;

        final List<String> matches = new ArrayList<>();
        enchantmentRegistry().keyStream().forEach(key -> {
            final String candidate = !qualified && Key.MINECRAFT_NAMESPACE.equals(key.namespace())
                    ? key.value() : key.asString();
            if (candidate.startsWith(lower)) {
                matches.add(candidate);
            }
        });
        matches.sort(null);
        return matches;
    }

    private static List<String> suggestLevels(final Enchantment enchantment, final String prefix) {
        // deliberaltely cuts at the normal max, but allows greater
        final int max = enchantment.getMaxLevel();

        final List<String> matches = new ArrayList<>();
        for (int level = MIN_LEVEL; level <= max; level++) {
            final String candidate = Integer.toString(level);
            if (candidate.startsWith(prefix)) {
                matches.add(candidate);
            }
        }
        return matches;
    }

    private static Enchantment resolveEnchantment(final String input) {
        final String key = input.toLowerCase(Locale.ROOT);
        return Key.parseable(key) ? enchantmentRegistry().get(Key.key(key)) : null;
    }

    private static Registry<Enchantment> enchantmentRegistry() {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
    }

}
