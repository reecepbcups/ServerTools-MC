package sh.reece.core.kit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Schedulers;
import sh.reece.utiltools.TimeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static sh.reece.utiltools.TextUtil.format;

/** {@code /kit}, {@code /kits}, {@code /createkit}, {@code /delkit}, {@code /showkit}, {@code /kitreset}. */
final class KitCommand extends BaseCommand implements Listener {

    private static final String NO_PERMISSION = "<red>You do not have permission to use this command.";
    private static final String NO_KIT_PERMISSION = "<red>You do not have access to the <white>%s</white> kit.";
    private static final String KIT_NOT_FOUND = "<red>Kit <white>%s</white> does not exist.";
    private static final String PLAYER_NOT_FOUND = "<red>Player <white>%s</white> not found.";
    private static final String CONSOLE_NEEDS_PLAYER = "<red>Usage: /kit \\<kit> \\<player>";

    private static final String USAGE_CREATEKIT = "<red>Usage: /createkit \\<name> \\<delay seconds, -1 for one time>";
    private static final String USAGE_DELKIT = "<red>Usage: /delkit \\<kit>";
    private static final String USAGE_SHOWKIT = "<red>Usage: /showkit \\<kit>";
    private static final String USAGE_KITRESET = "<red>Usage: /kitreset \\<kit> [player]";

    private static final String KIT_ONCE = "<red>You can't use the <white>%s</white> kit again.";
    private static final String KIT_TIMED = "<red>You can use the <white>%s</white> kit again in <white>%s</white>.";
    private static final String KIT_CANT_AFFORD = "<red>The <white>%s</white> kit costs <white>%s</white>.";
    private static final String KIT_NO_SPACE = "<red>Your inventory is too full for the <white>%s</white> kit.";
    private static final String KIT_NO_SPACE_OTHER = "<red>%s's inventory is too full for the <white>%s</white> kit.";
    private static final String KIT_DROPPED = "<yellow>Your inventory was full, some items were dropped on the ground.";
    private static final String KIT_RECEIVED = "<green>Received kit <white>%s</white>.";
    private static final String KIT_GIVEN = "<green>Gave kit <white>%s</white> to <white>%s</white>.";

    private static final String LIST_HEADER = "<gold>Kits<gray>: ";
    private static final String LIST_EMPTY = "<red>There are no kits available.";
    private static final String LIST_READY = "<white>%s";
    private static final String LIST_WAITING = "<gray><strikethrough>%s</strikethrough>";
    private static final String LIST_COST = " <gray>(%s)";
    private static final String LIST_SEPARATOR = "<gray>, ";

    private static final String CREATED = "<green>Created kit <white>%s</white> with %s items and a %s delay.";
    private static final String DELETED = "<green>Deleted kit <white>%s</white>.";
    private static final String SHOW_HEADER = "<gold>Kit <white>%s</white> contains:";
    private static final String SHOW_LINE = "<gray>- ";
    private static final String PREVIEW_TITLE = "<dark_gray>Kit: %s";
    private static final String RESET = "<green>Reset the <white>%s</white> kit cooldown for <white>%s</white>.";

    private final KitFeature feature;

    KitCommand(final Main plugin, final KitFeature feature) {
        super(plugin, KitFeature.SECTION, "kit");
        this.feature = feature;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        switch (label.toLowerCase(Locale.ROOT)) {
            case "createkit" -> create(sender, args);
            case "delkit", "deletekit" -> delete(sender, args);
            case "showkit" -> show(sender, args);
            case "kitreset" -> reset(sender, args);
            default -> {
                if (noPermission(sender, cmd)) return true;
                if (args.length == 0) list(sender);
                else claim(sender, args);
            }
        }
        return true;
    }

    // /kit <kit[,kit2]> [player]
    private void claim(final CommandSender sender, final String[] args) {
        final Player self = sender instanceof Player player ? player : null;
        if (self == null && args.length < 2) {
            sender.sendMessage(format(CONSOLE_NEEDS_PLAYER));
            return;
        }

        Player target = self;
        if (args.length > 1 && (self == null || hasPermission(sender, this.feature.othersPerm()))) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(format(PLAYER_NOT_FOUND, args[1]));
                return;
            }
        }

        // only claiming for yourself costs money and starts the cooldown. Gifts from staff or console are free.
        final boolean claiming = self != null && target == self;

        // check every kit before giving any, so a bad name in a list doesn't hand out half of it
        final List<Kit> kits = new ArrayList<>();
        for (final String name : args[0].split(",")) {
            final Kit kit = this.feature.kit(name.trim()).orElse(null);
            if (kit == null) {
                sender.sendMessage(format(KIT_NOT_FOUND, name));
                return;
            }
            if (self != null && !checkAllowed(self, kit, claiming)) {
                return;
            }
            kits.add(kit);
        }

        final Player receiver = target;
        Schedulers.entity(this.plugin, receiver, () -> {
            for (final Kit kit : kits) {
                if (self != null && !checkAllowed(self, kit, claiming)) continue;

                final KitFeature.GiveResult result = this.feature.give(receiver, kit);
                if (result == KitFeature.GiveResult.NO_SPACE) {
                    sender.sendMessage(receiver == self
                            ? format(KIT_NO_SPACE, kit.name())
                            : format(KIT_NO_SPACE_OTHER, receiver.getName(), kit.name()));
                    continue;
                }

                if (claiming) {
                    this.feature.markUsed(self, kit);
                    this.feature.charge(self, kit);
                }
                if (receiver != sender) {
                    sender.sendMessage(format(KIT_GIVEN, kit.name(), receiver.getName()));
                }
                receiver.sendMessage(format(KIT_RECEIVED, kit.name()));
                if (result == KitFeature.GiveResult.DROPPED_EXTRA) {
                    receiver.sendMessage(format(KIT_DROPPED));
                }
            }
        });
    }

    /** Kit permission, plus cooldown and cost when claiming for yourself. Sends the reason when it fails. */
    private boolean checkAllowed(final Player player, final Kit kit, final boolean claiming) {
        if (!this.feature.canUse(player, kit)) {
            player.sendMessage(format(NO_KIT_PERMISSION, kit.name()));
            return false;
        }
        if (!claiming) {
            return true;
        }

        final long next = this.feature.nextUse(player, kit);
        if (next < 0) {
            player.sendMessage(format(KIT_ONCE, kit.name()));
            return false;
        }
        if (next > 0) {
            final long seconds = Math.max(1, (next - System.currentTimeMillis() + 999) / 1000);
            player.sendMessage(format(KIT_TIMED, kit.name(), TimeUtil.formatDuration(seconds)));
            return false;
        }

        if (!this.feature.canAfford(player, kit)) {
            player.sendMessage(format(KIT_CANT_AFFORD, kit.name(), this.feature.formatCost(kit.cost())));
            return false;
        }
        return true;
    }

    private void list(final CommandSender sender) {
        final Player player = sender instanceof Player p ? p : null;

        Component line = Component.empty();
        boolean first = true;
        for (final Kit kit : this.feature.kits()) {
            String entry = LIST_READY;
            if (player != null) {
                if (!this.feature.canUse(player, kit)) continue;

                final long next = this.feature.nextUse(player, kit);
                if (next < 0 && this.feature.skipUsedOneTime()) continue;
                if (next != 0) entry = LIST_WAITING;
            }

            if (!first) line = line.append(format(LIST_SEPARATOR));
            first = false;
            line = line.append(format(entry, kit.name()));
            if (kit.cost() > 0) {
                line = line.append(format(LIST_COST, this.feature.formatCost(kit.cost())));
            }
        }

        sender.sendMessage(first ? format(LIST_EMPTY) : format(LIST_HEADER).append(line));
    }

    // /createkit <name> <delay>, saves the inventory exactly as is
    private void create(final CommandSender sender, final String[] args) {
        if (!hasPermission(sender, this.feature.createKitPerm())) {
            sender.sendMessage(format(NO_PERMISSION));
            return;
        }
        final Player player = playerOrNull(sender);
        if (player == null) return;

        if (args.length != 2) {
            sender.sendMessage(format(USAGE_CREATEKIT));
            return;
        }
        final long delay = args[1].equals("-1") ? -1 : TimeUtil.parseDuration(args[1]);
        if (delay < -1 || (delay == -1 && !args[1].equals("-1"))) {
            sender.sendMessage(format(USAGE_CREATEKIT));
            return;
        }

        final ItemStack[] contents = player.getInventory().getContents();
        final List<String> lines = new ArrayList<>();
        for (int slot = 0; slot < contents.length; slot++) {
            final ItemStack item = contents[slot];
            if (item != null && !item.getType().isAir()) {
                lines.add("slot:" + slot + " " + KitItems.serialize(item));
            }
        }

        final String name = Kit.sanitizeName(args[0]);
        this.feature.create(name, delay, lines);
        sender.sendMessage(format(CREATED, name, lines.size(),
                delay < 0 ? "one time" : TimeUtil.formatDuration(delay)));
    }

    private void delete(final CommandSender sender, final String[] args) {
        if (!hasPermission(sender, this.feature.deleteKitPerm())) {
            sender.sendMessage(format(NO_PERMISSION));
            return;
        }
        if (args.length != 1) {
            sender.sendMessage(format(USAGE_DELKIT));
            return;
        }
        final Kit kit = this.feature.kit(args[0]).orElse(null);
        if (kit == null) {
            sender.sendMessage(format(KIT_NOT_FOUND, args[0]));
            return;
        }

        this.feature.delete(kit);
        sender.sendMessage(format(DELETED, kit.name()));
    }

    private void show(final CommandSender sender, final String[] args) {
        if (!hasPermission(sender, this.feature.showKitPerm())) {
            sender.sendMessage(format(NO_PERMISSION));
            return;
        }
        if (args.length != 1) {
            sender.sendMessage(format(USAGE_SHOWKIT));
            return;
        }

        if (sender instanceof Player player && args[0].indexOf(',') < 0) {
            final Kit kit = this.feature.kit(args[0]).orElse(null);
            if (kit == null) {
                sender.sendMessage(format(KIT_NOT_FOUND, args[0]));
                return;
            }
            openPreview(player, kit);
            return;
        }

        for (final String name : args[0].split(",")) {
            final Kit kit = this.feature.kit(name).orElse(null);
            if (kit == null) {
                sender.sendMessage(format(KIT_NOT_FOUND, name));
                continue;
            }
            sender.sendMessage(format(SHOW_HEADER, kit.name()));
            for (final String line : kit.lines()) {
                // plain text, & codes in the line must show as written
                sender.sendMessage(format(SHOW_LINE).append(Component.text(line, NamedTextColor.WHITE)));
            }
        }
    }

    /** Read only chest showing the kit's items in order. */
    private void openPreview(final Player player, final Kit kit) {
        final List<ItemStack> items = this.feature.preview(player, kit);
        final int size = Math.min(54, Math.max(9, (items.size() + 8) / 9 * 9));

        final Preview holder = new Preview();
        final Inventory inventory = Bukkit.createInventory(holder, size, format(PREVIEW_TITLE, kit.name()));
        holder.inventory = inventory;
        for (int i = 0; i < Math.min(size, items.size()); i++) {
            inventory.setItem(i, items.get(i));
        }
        player.openInventory(inventory);
    }

    private static final class Preview implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return this.inventory;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPreviewClick(final InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof Preview) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPreviewDrag(final InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof Preview) {
            event.setCancelled(true);
        }
    }

    // /kitreset <kit> [player]
    private void reset(final CommandSender sender, final String[] args) {
        if (!hasPermission(sender, this.feature.kitResetPerm())) {
            sender.sendMessage(format(NO_PERMISSION));
            return;
        }
        final boolean console = !(sender instanceof Player);
        if (args.length < 1 || (console && args.length < 2)) {
            sender.sendMessage(format(USAGE_KITRESET));
            return;
        }
        final Kit kit = this.feature.kit(args[0]).orElse(null);
        if (kit == null) {
            sender.sendMessage(format(KIT_NOT_FOUND, args[0]));
            return;
        }

        OfflinePlayer target = console ? null : (Player) sender;
        if (args.length > 1 && hasPermission(sender, this.feature.kitResetOthersPerm())) {
            target = Bukkit.getOfflinePlayerIfCached(args[1]);
            if (target == null) {
                sender.sendMessage(format(PLAYER_NOT_FOUND, args[1]));
                return;
            }
        }
        if (target == null) {
            sender.sendMessage(format(NO_PERMISSION));
            return;
        }

        this.feature.reset(target, kit);
        sender.sendMessage(format(RESET, kit.name(), target.getName()));
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        final String command = label.toLowerCase(Locale.ROOT);
        if (command.equals("kits") || command.equals("createkit")) {
            return List.of();
        }
        if (args.length == 1) {
            final String prefix = args[0].toLowerCase(Locale.ROOT);
            final boolean onlyUsable = command.equals("kit") && sender instanceof Player;
            final List<String> names = new ArrayList<>();
            for (final Kit kit : this.feature.kits()) {
                if (onlyUsable && !this.feature.canUse((Player) sender, kit)) continue;
                if (kit.name().startsWith(prefix)) names.add(kit.name());
            }
            return names;
        }
        if (args.length == 2 && (command.equals("kit") || command.equals("kitreset"))) {
            return super.onTabComplete(sender, cmd, label, new String[]{args[1]});
        }
        return List.of();
    }
}
