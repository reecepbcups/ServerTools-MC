package sh.reece.core;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemLore;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemTooltipDisplay;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.Enchantment;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Rewrites enchant tooltips in outgoing item packets so over-vanilla levels show real numbers.
 *
 * <p>Vanilla only ships the {@code enchantment.level.1} through {@code enchantment.level.10}
 * translations, so the client-rendered enchant line leaks the raw key (e.g.
 * {@code enchantment.level.11}) for anything higher. We can't fix a single line client-side, so for
 * any item carrying an over-vanilla enchant we hide the vanilla enchant tooltip and write our own
 * lore lines with plain numbers. Normal items (all levels 1-10) are left untouched so they keep
 * their roman numerals. This only rewrites the packet copy - the real item NBT is never changed.
 */
public class EnchantTooltipListener extends PacketListenerAbstract {

    // vanilla only translates enchantment.level.1 .. .10; anything higher leaks the raw key
    private static final int VANILLA_MAX_TRANSLATED_LEVEL = 10;

    public EnchantTooltipListener() {
        super(PacketListenerPriority.NORMAL);
    }

    @Override
    public void onPacketSend(final PacketSendEvent event) {
        final PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Server.SET_SLOT) {
            final WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);
            if (rewrite(wrapper.getItem())) {
                event.markForReEncode(true);
            }
        } else if (type == PacketType.Play.Server.WINDOW_ITEMS) {
            final WrapperPlayServerWindowItems wrapper = new WrapperPlayServerWindowItems(event);
            boolean changed = false;
            for (final ItemStack item : wrapper.getItems()) {
                changed |= rewrite(item);
            }
            if (changed) {
                event.markForReEncode(true);
            }
        }
    }

    /**
     * Rewrites the item's enchant tooltip in place. Returns true if anything changed so the caller
     * knows to re-encode the packet.
     */
    private static boolean rewrite(final ItemStack item) {
        if (item == null || item.isEmpty()) {
            return false;
        }

        final List<Enchantment> enchants = item.getEnchantments();
        if (enchants.isEmpty() || enchants.stream().noneMatch(e -> e.getLevel() > VANILLA_MAX_TRANSLATED_LEVEL)) {
            return false; // nothing over vanilla - leave the client's roman numerals alone
        }

        // our enchant lines go above any lore the item already has, matching vanilla order
        final List<Component> lore = new ArrayList<>();
        for (final Enchantment enchant : enchants) {
            lore.add(enchantLine(enchant));
        }
        lore.addAll(item.getComponentOr(ComponentTypes.LORE, ItemLore.EMPTY).getLines());
        item.setComponent(ComponentTypes.LORE, new ItemLore(lore));

        // hide the client's own enchant tooltip so it doesn't render alongside ours
        final ItemTooltipDisplay display = item.getComponentOr(
                ComponentTypes.TOOLTIP_DISPLAY, new ItemTooltipDisplay(false, new LinkedHashSet<>()));
        final Set<com.github.retrooper.packetevents.protocol.component.ComponentType<?>> hidden =
                new LinkedHashSet<>(display.getHiddenComponents());
        hidden.add(ComponentTypes.ENCHANTMENTS);
        item.setComponent(ComponentTypes.TOOLTIP_DISPLAY,
                new ItemTooltipDisplay(display.isHideTooltip(), hidden));

        return true;
    }

    /**
     * Enchant name (from the client's own translation) followed by a plain number, mirroring
     * vanilla's colouring and its habit of dropping the level for single-level enchants.
     */
    private static Component enchantLine(final Enchantment enchant) {
        Component line = enchant.getType().getDescription().colorIfAbsent(NamedTextColor.GRAY);
        final int level = enchant.getLevel();
        final int maxLevel = enchant.getType().getDefinition().getMaxLevel();
        if (level != 1 || maxLevel != 1) {
            line = line.append(Component.space()).append(Component.text(level));
        }
        // lore defaults to italic; vanilla enchant lines are not
        return line.decoration(TextDecoration.ITALIC, false);
    }
}
