package sh.reece.cmds;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import sh.reece.tools.Main;
import sh.reece.utiltools.Util;

public class ItemDB implements CommandExecutor {

    public ItemDB(Main plugin) {
        plugin.getCommand("itemdb").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            Util.coloredMessage(sender, "&cYou must be a player to use this command.");
            return true;
        }

        getItemInfo(sender);
        return true;
    }

    @SuppressWarnings("deprecation")
    private void getItemInfo(CommandSender sender) {
		// get item in players hand
		final Player p = (Player) sender;
		final org.bukkit.inventory.ItemStack item = p.getItemInHand();
		final Material mat = item.getType();	

        // get the item the player is looking at
        final org.bukkit.util.RayTraceResult ray = p.rayTraceBlocks(10);
        Material blockMat = null;
        org.bukkit.block.Block block = null;
        if (ray != null) {
            block = ray.getHitBlock();
            if (block != null) {
                blockMat = block.getType();
            }
        }
              

        // print the itemName & amount held
        Util.coloredMessage(sender, "\n&fYou are holding &c" + item.getAmount() + "&fx &c" + mat);
        // print out what they are looking at
        if(blockMat != null) {
            Util.coloredMessage(sender, "&fYou are looking at &c" + blockMat);
        }
        
	}
    
}
