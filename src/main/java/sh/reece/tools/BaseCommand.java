package sh.reece.tools;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import sh.reece.utiltools.Util;

public abstract class BaseCommand implements CommandExecutor, TabCompleter {

    protected final Main plugin;
    protected final ConfigUtils configUtils;
    protected final String section;
    protected final String permission;
    private final boolean enabled;

    protected BaseCommand(Main plugin, String section, String... commands) {
        this.plugin = plugin;
        this.section = section;
        this.configUtils = plugin.getConfigUtils();
        this.enabled = configUtils.enabledInConfig(section + ".Enabled");

        if (enabled) {
            String permPath = section + ".Permission";
            this.permission = plugin.getConfig().contains(permPath)
                ? plugin.getConfig().getString(permPath, "") : "";
            for (String cmd : commands) {
                PluginCommand pc = plugin.getCommand(cmd);
                if (pc != null) {
                    pc.setExecutor(this);
                    pc.setTabCompleter(this);
                }
            }
            if (this instanceof Listener) {
                Bukkit.getPluginManager().registerEvents((Listener) this, plugin);
            }
        } else {
            this.permission = "";
            for (String cmd : commands) {
                AlternateCommandHandler.addDisableCommand(cmd);
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    protected boolean hasPermission(CommandSender sender, String perm) {
        return perm == null || perm.isEmpty() || sender.isOp() || sender.hasPermission(perm);
    }

    protected boolean noPermission(CommandSender sender, Command cmd) {
        if (!hasPermission(sender, permission)) {
            sender.sendMessage(Util.color("&cYou do not have access to &n/" + cmd.getName() + "&c."));
            return true;
        }
        return false;
    }

    protected Player playerOrNull(CommandSender sender) {
        if (sender instanceof Player) {
            return (Player) sender;
        }
        Util.consoleMSG("&cThis command can only be run by a player.");
        return null;
    }

    /**
     * Resolve self (no args) or target player (first arg with .others perm check).
     * Returns null if sender lacks permission or target is offline (message already sent).
     */
    protected Player resolveTarget(CommandSender sender, String[] args, Command cmd) {
        if (args.length == 0) {
            return playerOrNull(sender);
        }
        if (!hasPermission(sender, permission.isEmpty() ? "" : permission + ".others")) {
            sender.sendMessage(Util.color("&cYou do not have access to &n/" + cmd.getName() + "&c for others."));
            return null;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            Util.coloredMessage(sender, "&f[&c!&f] &cTarget " + args[0] + " is not online.");
            return null;
        }
        return target;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix))
                .sorted()
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
