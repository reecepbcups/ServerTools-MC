package sh.reece.tools;

import org.bukkit.entity.Player;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import sh.reece.GUI.ChatColor;
import sh.reece.core.Nickname;
import sh.reece.cmds.Visibility;
import sh.reece.moderation.CommandSpy;
import sh.reece.moderation.StaffAFK;
import sh.reece.utiltools.Util;

public class ServerToolsPlaceholders extends PlaceholderExpansion {

	// %stools_% and %servertools_% get everything. %essentials_% only answers what we replace, so other
	// essentials_ placeholders stay unparsed instead of showing an error.
	private final String identifier;

	public ServerToolsPlaceholders(String identifier) {
		this.identifier = identifier;
	}

	public String getIdentifier() {
		return identifier;
	}

	// keep the expansion registered through /papi reload
	@Override
	public boolean persist() {
		return true;
	}

	public String getAuthor() {
		return "Reecepbcups";
	}

	public String getVersion() {
		return "1.0";
	}
	
	
	public String onPlaceholderRequest(Player player, String identifier) {
		
		if (identifier == null) {
			return null;
		}

		// fast path: nearly all placeholders have no "_" arg, so avoid split()'s
		// String[] allocation on every per-player per-tick request.
		if (player != null && identifier.equals("nickname")) { // %stools_nickname%
			return Nickname.displayName(player);
		}
		if (player != null && identifier.equals("nickname_stripped")) {
			return org.bukkit.ChatColor.stripColor(Nickname.displayName(player));
		}
		if (this.identifier.equals("essentials")) {
			return null;
		}

		int underscore = identifier.indexOf('_');
		String key = underscore == -1 ? identifier : identifier.substring(0, underscore);

		switch (key) {

			case "isvisible": // %stools_isvisible%
				return Visibility.isPlayerHidden(player) ? "true" : "false";


			case "age": // %stools_age_1622318400%
				// 2nd identifier = epoch time ( %stools_age_https://www.epochconverter.com/% )
				if(underscore != -1){
					return Util.placeholderTimeRequest(identifier.substring(underscore + 1));
				}
				return "%stools_age_<EPOCHTIME>%";
			
			case "commandspy":
				return CommandSpy.isWatching(player.getUniqueId()) ? "on" : "off";

			case "chatcolor": // returns & code "&e" for example
				return ChatColor.getColor(player.getUniqueId().toString());

			case "staffafk":
				return StaffAFK.isStaffAfk(player.getUniqueId()) ? "true" : "false";

			default:
				return "STOOLS-PAPI-ERROR";
				
		}
	}
}

