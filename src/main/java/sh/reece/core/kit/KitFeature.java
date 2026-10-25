package sh.reece.core.kit;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.reece.tools.Main;
import sh.reece.tools.Unloadable;
import sh.reece.utiltools.Schedulers;
import sh.reece.utiltools.Util;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class KitFeature implements Unloadable {

    private static final Logger LOGGER = LoggerFactory.getLogger(KitFeature.class);

    static final String SECTION = "Core.Kits";
    static final String KITS_FILE = "Kits.yml";
    static final String KITS_DIR = "kits";

    private static final long SAVE_PERIOD_TICKS = 20L * 60 * 5;

    // PlayerInventory indexes for boots, leggings, chestplate, helmet, offhand
    private static final int FIRST_EQUIPMENT_SLOT = 36;
    private static final int LAST_EQUIPMENT_SLOT = 40;

    enum GiveResult { GIVEN, DROPPED_EXTRA, NO_SPACE }

    private final Main plugin;
    private final boolean enabled;

    private volatile Map<String, Kit> kits = Map.of();
    private KitData data;
    private ScheduledTask saveTask;

    private String kitPermPrefix;
    private String othersPerm;
    private String exemptDelayPerm;
    private String createKitPerm;
    private String deleteKitPerm;
    private String showKitPerm;
    private String kitResetPerm;
    private String kitResetOthersPerm;
    private boolean autoEquip;
    private boolean dropIfFull;
    private boolean skipUsedOneTime;
    private boolean allowUnsafe;

    public KitFeature(final Main plugin) {
        this.plugin = plugin;
        this.enabled = new KitCommand(plugin, this).isEnabled();
        if (!this.enabled) {
            return;
        }

        final File essentials = new File(plugin.getDataFolder().getParentFile(), "Essentials");
        copyKitsFrom(new File(essentials, "kits.yml"));
        plugin.getConfigUtils().createConfig(KITS_FILE);

        this.data = new KitData(new File(plugin.getDataFolder(), KitData.FILE));
        final boolean firstRun = !this.data.exists();
        this.data.load();
        if (firstRun) {
            final File userdata = new File(essentials, "userdata");
            if (userdata.isDirectory()) {
                Schedulers.async(plugin, () -> {
                    final int players = this.data.importUserdata(userdata);
                    this.data.saveIfDirty();
                    LOGGER.info("Imported kit cooldowns for {} players", players);
                });
            }
        }

        load();
        this.saveTask = Schedulers.globalTimer(plugin,
                () -> Schedulers.async(plugin, this.data::saveIfDirty), SAVE_PERIOD_TICKS, SAVE_PERIOD_TICKS);
    }

    /** First start only: an existing kits file moves over as is, since the format is the same. */
    private void copyKitsFrom(final File existing) {
        final File ours = new File(this.plugin.getDataFolder(), KITS_FILE);
        if (ours.exists() || !existing.isFile()) {
            return;
        }
        try {
            Files.createDirectories(ours.getParentFile().toPath());
            Files.copy(existing.toPath(), ours.toPath());
            LOGGER.info("Copied kits from {}", existing.getPath());
        } catch (final IOException e) {
            LOGGER.warn("Failed to copy kits from {}", existing.getPath(), e);
        }
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    /** Re-reads the settings in config.yml plus every kit in Kits.yml and kits/*.yml. */
    public void load() {
        final FileConfiguration config = this.plugin.getConfig();
        this.kitPermPrefix = config.getString(SECTION + ".KitPermPrefix", "essentials.kits.");
        this.othersPerm = config.getString(SECTION + ".OthersPerm", "essentials.kit.others");
        this.exemptDelayPerm = config.getString(SECTION + ".ExemptDelayPerm", "essentials.kit.exemptdelay");
        this.createKitPerm = config.getString(SECTION + ".CreateKitPerm", "essentials.createkit");
        this.deleteKitPerm = config.getString(SECTION + ".DeleteKitPerm", "essentials.delkit");
        this.showKitPerm = config.getString(SECTION + ".ShowKitPerm", "essentials.showkit");
        this.kitResetPerm = config.getString(SECTION + ".KitResetPerm", "essentials.kitreset");
        this.kitResetOthersPerm = config.getString(SECTION + ".KitResetOthersPerm", "essentials.kitreset.others");
        this.autoEquip = config.getBoolean(SECTION + ".AutoEquip", false);
        this.dropIfFull = config.getBoolean(SECTION + ".DropItemsIfFull", true);
        this.skipUsedOneTime = config.getBoolean(SECTION + ".SkipUsedOneTimeKits", false);
        this.allowUnsafe = config.getBoolean(SECTION + ".AllowUnsafeEnchantments", false);

        final Map<String, Kit> parsed = new LinkedHashMap<>();
        readKits(new File(this.plugin.getDataFolder(), KITS_FILE), parsed);

        final File[] extra = new File(this.plugin.getDataFolder(), KITS_DIR).listFiles((d, n) -> n.endsWith(".yml"));
        if (extra != null) {
            Arrays.sort(extra);
            for (final File file : extra) {
                readKits(file, parsed);
            }
        }

        this.kits = parsed;
        LOGGER.info("Loaded {} kits", parsed.size());
    }

    private void readKits(final File file, final Map<String, Kit> into) {
        final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        final ConfigurationSection section = yaml.getConfigurationSection("kits");
        if (section == null) {
            return;
        }
        // costs pasted in from an old command-costs block still count
        final ConfigurationSection costs = yaml.getConfigurationSection("command-costs");

        for (final String key : section.getKeys(false)) {
            final ConfigurationSection kit = section.getConfigurationSection(key);
            if (kit == null) continue;

            final String name = Kit.sanitizeName(key);
            final double fallbackCost = costs == null ? 0 : costs.getDouble("kit-" + name);
            final List<String> lines = kit.getStringList("items");

            final List<Kit.Entry> entries = new ArrayList<>(lines.size());
            for (final String line : lines) {
                try {
                    entries.add(parseEntry(line));
                } catch (final RuntimeException e) {
                    LOGGER.warn("Kit {}: skipping '{}': {}", name, line, e.getMessage());
                }
            }

            into.put(name, new Kit(name, kit.getDouble("delay", 0), kit.getDouble("cost", fallbackCost),
                    List.copyOf(entries), List.copyOf(lines), file));
        }
    }

    private Kit.Entry parseEntry(final String line) {
        if (line.startsWith("$")) {
            return new Kit.Entry(Kit.Kind.MONEY, line.substring(1), -1, null);
        }
        if (line.startsWith("/")) {
            return new Kit.Entry(Kit.Kind.COMMAND, line.substring(1), -1, null);
        }

        int slot = -1;
        String raw = line;
        if (raw.startsWith("slot:")) {
            final int space = raw.indexOf(' ');
            if (space > 0) {
                final String number = raw.substring("slot:".length(), space);
                slot = Util.isInt(number) ? Integer.parseInt(number) : -1;
                raw = raw.substring(space + 1);
            }
        }

        final ItemStack template = hasPlaceholders(raw) ? null : KitItems.parse(raw, this.allowUnsafe);
        return new Kit.Entry(Kit.Kind.ITEM, raw, slot, template);
    }

    private static boolean hasPlaceholders(final String raw) {
        return raw.indexOf('{') >= 0 || (Main.isPAPIEnabled() && raw.indexOf('%') >= 0);
    }

    private static String expand(String raw, final Player player) {
        if (raw.indexOf('{') >= 0) {
            final String display = PlainTextComponentSerializer.plainText().serialize(player.displayName());
            raw = raw.replace("{USERNAME}", player.getName())
                    .replace("{player}", player.getName())
                    .replace("{PLAYER}", display)
                    .replace("{DISPLAYNAME}", display)
                    .replace("{WORLD}", player.getWorld().getName());
        }
        if (Main.isPAPIEnabled() && raw.indexOf('%') >= 0) {
            raw = PlaceholderAPI.setPlaceholders(player, raw);
        }
        return raw;
    }

    /**
     * Puts the kit in the player's inventory. Everything is laid out on a copy first, so a NO_SPACE result
     * leaves the real inventory untouched. Must run on the player's thread.
     */
    GiveResult give(final Player player, final Kit kit) {
        final List<ItemStack> loose = new ArrayList<>();
        final Map<Integer, ItemStack> slotted = new LinkedHashMap<>();
        final List<String> money = new ArrayList<>();
        final List<String> commands = new ArrayList<>();

        for (final Kit.Entry entry : kit.entries()) {
            switch (entry.kind()) {
                case MONEY -> money.add(expand(entry.raw(), player));
                case COMMAND -> commands.add(expand(entry.raw(), player));
                case ITEM -> {
                    final ItemStack item = item(kit, entry, player);
                    if (item == null) continue;
                    if (entry.slot() < 0 || slotted.putIfAbsent(entry.slot(), item) != null) {
                        loose.add(item);
                    }
                }
            }
        }

        final PlayerInventory inventory = player.getInventory();
        final Inventory layout = Bukkit.createInventory(null, FIRST_EQUIPMENT_SLOT);
        layout.setContents(inventory.getStorageContents());

        final Map<Integer, ItemStack> equip = new HashMap<>();
        slotted.forEach((slot, item) -> {
            if (slot < FIRST_EQUIPMENT_SLOT) {
                if (isEmpty(layout.getItem(slot))) layout.setItem(slot, item);
                else loose.add(item);
            } else if (slot <= LAST_EQUIPMENT_SLOT && isEmpty(inventory.getItem(slot)) && !equip.containsKey(slot)) {
                equip.put(slot, item);
            } else {
                loose.add(item);
            }
        });

        if (this.autoEquip) {
            for (final Iterator<ItemStack> it = loose.iterator(); it.hasNext(); ) {
                final ItemStack item = it.next();
                final int slot = armorSlot(item);
                if (slot >= 0 && isEmpty(inventory.getItem(slot)) && !equip.containsKey(slot)) {
                    equip.put(slot, item);
                    it.remove();
                }
            }
        }

        final Map<Integer, ItemStack> leftover = layout.addItem(loose.toArray(new ItemStack[0]));
        if (!leftover.isEmpty() && !this.dropIfFull) {
            return GiveResult.NO_SPACE;
        }

        inventory.setStorageContents(layout.getContents());
        equip.forEach(inventory::setItem);
        for (final ItemStack item : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }

        for (final String amount : money) {
            deposit(player, amount);
        }
        for (final String command : commands) {
            Schedulers.global(this.plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
        }
        return leftover.isEmpty() ? GiveResult.GIVEN : GiveResult.DROPPED_EXTRA;
    }

    /** The entry's item for this player, null (and logged) when it can't be built. */
    private ItemStack item(final Kit kit, final Kit.Entry entry, final Player player) {
        try {
            return entry.template() != null
                    ? entry.template().clone()
                    : KitItems.parse(expand(entry.raw(), player), this.allowUnsafe);
        } catch (final RuntimeException e) {
            LOGGER.warn("Kit {}: skipping '{}': {}", kit.name(), entry.raw(), e.getMessage());
            return null;
        }
    }

    /** What the kit gives, for the /showkit menu. Money and commands show as labelled paper. */
    List<ItemStack> preview(final Player player, final Kit kit) {
        final List<ItemStack> items = new ArrayList<>();
        for (final Kit.Entry entry : kit.entries()) {
            switch (entry.kind()) {
                case ITEM -> {
                    final ItemStack item = item(kit, entry, player);
                    if (item != null) items.add(item);
                }
                case MONEY -> items.add(label(Material.GOLD_INGOT, "&6$" + entry.raw()));
                case COMMAND -> items.add(label(Material.PAPER, "&7/" + expand(entry.raw(), player)));
            }
        }
        return items;
    }

    private static ItemStack label(final Material material, final String name) {
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Util.color(name));
        item.setItemMeta(meta);
        return item;
    }

    private static boolean isEmpty(final ItemStack item) {
        return item == null || item.getType().isAir();
    }

    private static int armorSlot(final ItemStack item) {
        return switch (item.getType().getEquipmentSlot()) {
            case HEAD -> 39;
            case CHEST -> 38;
            case LEGS -> 37;
            case FEET -> 36;
            default -> -1;
        };
    }

    private void deposit(final Player player, final String amount) {
        final double value;
        try {
            value = new BigDecimal(amount.trim().replace(",", "")).doubleValue();
        } catch (final NumberFormatException e) {
            LOGGER.warn("Kit money line '${}' isn't a number", amount);
            return;
        }
        final Economy economy = economy();
        if (economy == null) {
            LOGGER.warn("Kit gives ${} but no economy is installed", amount);
            return;
        }
        economy.depositPlayer(player, value);
    }

    /** True when the kit is free or the player can pay for it. */
    boolean canAfford(final OfflinePlayer player, final Kit kit) {
        if (kit.cost() <= 0) return true;
        final Economy economy = economy();
        return economy == null || economy.has(player, kit.cost());
    }

    void charge(final OfflinePlayer player, final Kit kit) {
        if (kit.cost() <= 0) return;
        final Economy economy = economy();
        if (economy != null) {
            economy.withdrawPlayer(player, kit.cost());
        }
    }

    String formatCost(final double cost) {
        final Economy economy = economy();
        return economy == null ? Util.formatNumber(cost) : economy.format(cost);
    }

    // Vault is a soft dependency, so never touch its classes unless the plugin is there
    private static Economy economy() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            return null;
        }
        final RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
        return provider == null ? null : provider.getProvider();
    }

    Collection<Kit> kits() {
        return this.kits.values();
    }

    Optional<Kit> kit(final String name) {
        return Optional.ofNullable(this.kits.get(Kit.sanitizeName(name)));
    }

    /** Same as {@link Kit#nextUse} but honours the exempt permission. */
    long nextUse(final Player player, final Kit kit) {
        if (player.hasPermission(this.exemptDelayPerm)) return 0;
        return kit.nextUse(this.data.lastUsed(player.getUniqueId(), kit.name()), System.currentTimeMillis());
    }

    void markUsed(final OfflinePlayer player, final Kit kit) {
        this.data.setLastUsed(player.getUniqueId(), kit.name(), System.currentTimeMillis());
    }

    void reset(final OfflinePlayer player, final Kit kit) {
        this.data.setLastUsed(player.getUniqueId(), kit.name(), 0);
    }

    /** Writes a kit into Kits.yml, replacing one with the same name. */
    void create(final String name, final long delay, final List<String> lines) {
        final File file = new File(this.plugin.getDataFolder(), KITS_FILE);
        final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set("kits." + name + ".delay", delay);
        yaml.set("kits." + name + ".items", lines);
        saveAndReload(yaml, file);
    }

    void delete(final Kit kit) {
        final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(kit.source());
        final ConfigurationSection section = yaml.getConfigurationSection("kits");
        if (section != null) {
            // the key may not be lower case in the file
            for (final String key : section.getKeys(false)) {
                if (Kit.sanitizeName(key).equals(kit.name())) {
                    section.set(key, null);
                }
            }
        }
        saveAndReload(yaml, kit.source());
    }

    private void saveAndReload(final YamlConfiguration yaml, final File file) {
        try {
            yaml.save(file);
        } catch (final IOException e) {
            LOGGER.error("Failed to save {}", file.getName(), e);
        }
        load();
    }

    boolean canUse(final Player player, final Kit kit) {
        return player.hasPermission(this.kitPermPrefix + kit.name());
    }

    String othersPerm() { return this.othersPerm; }
    String createKitPerm() { return this.createKitPerm; }
    String deleteKitPerm() { return this.deleteKitPerm; }
    String showKitPerm() { return this.showKitPerm; }
    String kitResetPerm() { return this.kitResetPerm; }
    String kitResetOthersPerm() { return this.kitResetOthersPerm; }
    boolean skipUsedOneTime() { return this.skipUsedOneTime; }

    @Override
    public void onUnload() {
        if (this.saveTask != null) {
            this.saveTask.cancel();
        }
        if (this.data != null) {
            this.data.saveIfDirty();
        }
    }
}
