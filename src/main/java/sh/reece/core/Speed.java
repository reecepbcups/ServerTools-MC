package sh.reece.core;

import sh.reece.tools.BaseCommand;
import sh.reece.tools.Main;
import sh.reece.utiltools.Util;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Speed extends BaseCommand {

	private String FlyPerm;
	private String WalkPerm;

	public Speed(Main instance) {
		super(instance, "Commands.Speed", "speed");
		if (isEnabled()) {
			FlyPerm = plugin.getConfig().getString(section + ".FlyPermission");
			WalkPerm = plugin.getConfig().getString(section + ".WalkPermission");
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player p = (Player) sender;

		float FloatSpeed;
		if (args.length < 2) {

			if (args.length == 1) {
				char character = args[0].charAt(0);
				if (Character.isDigit(character)) {
					if (p.isFlying()) {
						setFlySpeed(p, getMoveSpeed(p, args[0]));
					} else {
						setWalkSpeed(p, getMoveSpeed(p, args[0]));
					}
				}
			} else {
				sendHelpMenu(p);
			}

			return true;

		} else {

			if (args[1].toLowerCase().equalsIgnoreCase("reset")) {
				switch (args[0]) {
				case "walk":
					resetSpeed(p, "walk");
					break;
				case "fly":
					resetSpeed(p, "fly");
					break;
				default:
					break;
				}
				return true;
			}

			FloatSpeed = getMoveSpeed(p, args[1]);
			if (FloatSpeed == 0f) { return true; }
		}

		switch (args[0]) {
		case "walk":
			setWalkSpeed(p, FloatSpeed);
			return true;
		case "fly":
			setFlySpeed(p, FloatSpeed);
			return true;
		default:
			sendHelpMenu(p);
			return true;
		}
	}

	public void resetSpeed(Player p, String SPEED_TYPES) {
		String FinalMSG = configUtils.lang("SPEED_RESET");

		switch (SPEED_TYPES.toLowerCase()) {
		case "walk":
			p.setWalkSpeed(0.2f);
			FinalMSG = FinalMSG.replace("%TYPE%", "Walk");
			break;
		case "fly":
			p.setFlySpeed(0.1f);
			FinalMSG = FinalMSG.replace("%TYPE%", "Fly");
			break;
		default:
			break;
		}
		Util.coloredMessage(p, FinalMSG);
	}

	public void setFlySpeed(Player p, Float speed) {
		if (!(p.hasPermission(FlyPerm))) {
			Util.coloredMessage(p, "&cNo Permission to use /speed fly :(");
			return;
		}
		p.setFlySpeed(speed);
		Util.coloredMessage(p, configUtils.lang("SPEED_FLY").replace("%speed%", (speed * 10) + ""));
	}
	public void setWalkSpeed(Player p, Float speed) {
		if (!(p.hasPermission(WalkPerm))) {
			Util.coloredMessage(p, "&cNo Permission to use /speed walk :(");
			return;
		}
		p.setWalkSpeed(speed);
		Util.coloredMessage(p, configUtils.lang("SPEED_WALK").replace("%speed%", (speed * 10) + ""));
	}

	public float getMoveSpeed(Player player, String MoveSpeed) {
		float FloatSpeed;
		try {
			FloatSpeed = Float.parseFloat(MoveSpeed);
			if (FloatSpeed > 10f) {
				FloatSpeed = 10f;
			} else if (FloatSpeed < 0.0001f) {
				FloatSpeed = 0.0001f;
			}
		} catch (Exception e) {
			Util.coloredMessage(player, "&c&n" + MoveSpeed + "&c is not a number!");
			return 0f;
		}

		return FloatSpeed / 10;
	}

	public void sendHelpMenu(Player p) {
		Util.coloredMessage(p, "&f/speed [0-10]");
		Util.coloredMessage(p, "&f/speed walk &7<speed/reset>");
		Util.coloredMessage(p, "&f/speed fly &7<speed/reset>");
	}

}
