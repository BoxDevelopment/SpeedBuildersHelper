package box.com.speedbuilderhelper;

import box.com.speedbuilderhelper.model.BuildRecord;
import box.com.speedbuilderhelper.utils.PlayerUtils;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

import java.util.List;

public class CMDS extends CommandBase {

    @Override
    public String getCommandName() {
        return "speedbuilders";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/speedbuilders";
    }

    @Override
    public List<String> getCommandAliases() {
        return java.util.Arrays.asList("sb", "speedbuilders");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            PlayerUtils.sendLine();
            PlayerUtils.sendMessageWithPing("§eSpeed-Builder Helper Commands");
            PlayerUtils.sendMessage(" §3/speedbuilders debug§7: enables / disables debugging");
            PlayerUtils.sendMessage(" §3/speedbuilders toggle§7: enables / disables speed-builder helper");
            PlayerUtils.sendMessage(" §3/speedbuilders times§7: shows your best times sorted by speed");
            PlayerUtils.sendMessage(" §3/speedbuilders showtime§7: shows your best time for the theme you are currently playing");
            PlayerUtils.sendMessage(" §3/speedbuilders overview§7: shows all new best times achieved this session");
            PlayerUtils.sendMessage(" §3/speedbuilders reset§7: clears the session best times list");
            PlayerUtils.sendLine();
        } else if (args[0].equalsIgnoreCase("debug")) {
            SpeedBuilderHelper.Debug = !SpeedBuilderHelper.Debug;
            PlayerUtils.sendMessageWithPing("&eDebug has been " + (SpeedBuilderHelper.Debug ? "&aenabled" : "&cdisabled") + "&e.");
            SpeedBuilderHelper.saveConfig();
        } else if (args[0].equalsIgnoreCase("toggle")) {
            SpeedBuilderHelper.Activated = !SpeedBuilderHelper.Activated;
            PlayerUtils.sendMessageWithPing("&espeed-builder helper has been " + (SpeedBuilderHelper.Activated ? "&aenabled" : "&cdisabled") + "&e.");
            SpeedBuilderHelper.saveConfig();
        } else if (args[0].equalsIgnoreCase("showtime")) {
            SpeedBuilderHelper.StartingMessage = !SpeedBuilderHelper.StartingMessage;
            PlayerUtils.sendMessageWithPing("&eShow Times has been " + (SpeedBuilderHelper.StartingMessage ? "&aenabled" : "&cdisabled") + "&e.");
            SpeedBuilderHelper.saveConfig();
        } else if (args[0].equalsIgnoreCase("times")) {
            List<SpeedBuilderHelper.BuildRecord> times = SpeedBuilderHelper.getInstance().getTimes();
            if (times.isEmpty()) {
                PlayerUtils.sendError("§cNo times recorded yet!");
                return;
            }

            if (args.length > 1) {
                String searchTheme = args[1];
                times.removeIf(record -> !record.theme.equalsIgnoreCase(searchTheme));
                if (times.isEmpty()) {
                    PlayerUtils.sendError("§cNo times found for theme: " + searchTheme);
                    return;
                }
            }

            times.sort((a, b) -> Double.compare(a.bestTime, b.bestTime));

            PlayerUtils.sendLine();
            PlayerUtils.sendMessageWithPing("§6Best Times:");
            for (SpeedBuilderHelper.BuildRecord record : times) {
                String variant = record.variant.isEmpty() ? "" : " (" + record.variant + ")";
                PlayerUtils.sendMessage(String.format(" §b%s%s §7(%s): §a%.2fs",
                        record.theme,
                        variant,
                        record.difficulty,
                        record.bestTime));
            }
            PlayerUtils.sendLine();
        } else if (args[0].equalsIgnoreCase("overview")) {
            SpeedBuilderHelper.getInstance().showSessionBestTimesOverview();
        } else if (args[0].equalsIgnoreCase("reset")) {
            SpeedBuilderHelper.getInstance().clearSessionBestTimes();
        } else {
            PlayerUtils.sendError("Not a valid command!");
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return -1;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }
}
