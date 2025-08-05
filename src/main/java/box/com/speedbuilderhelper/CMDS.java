package box.com.speedbuilderhelper;

import box.com.speedbuilderhelper.config.ConfigManager;
import box.com.speedbuilderhelper.managers.BuildTracker;
import box.com.speedbuilderhelper.managers.TimeManager;
import box.com.speedbuilderhelper.model.BuildRecord;
import box.com.speedbuilderhelper.utils.PlayerUtils;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

import java.util.List;
import java.util.Map;

public class CMDS extends CommandBase {

    private final SpeedBuilderHelper main;

    public CMDS(SpeedBuilderHelper main) {
        this.main = main;
    }

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
        ConfigManager configManager = main.getConfigManager();
        TimeManager timeManager = main.getTimeManager();
        BuildTracker buildTracker = main.getBuildTracker();

        if (args.length == 0) {
            PlayerUtils.sendLine();
            PlayerUtils.sendMessageWithPing("§eSpeed-Builder Helper Commands");
            PlayerUtils.sendMessage(" §3/speedbuilders debug§7: enables / disables debugging");
            PlayerUtils.sendMessage(" §3/speedbuilders toggle§7: enables / disables speed-builder helper");
            PlayerUtils.sendMessage(" §3/speedbuilders setname§7: sets your username to what you input");
            PlayerUtils.sendMessage(" §3/speedbuilders times§7: shows your best times sorted by speed");
            PlayerUtils.sendMessage(" §3/speedbuilders showtime§7: shows your best time for the theme you are currently playing");
            PlayerUtils.sendMessage(" §3/speedbuilders overview§7: shows all new best times achieved this session");
            PlayerUtils.sendMessage(" §3/speedbuilders reset§7: clears the session best times list");
            PlayerUtils.sendLine();
        } else if (args[0].equalsIgnoreCase("debug")) {
            configManager.setDebug(!configManager.isDebug());
            PlayerUtils.sendMessageWithPing("&eDebug has been " + (configManager.isDebug() ? "&aenabled" : "&cdisabled") + "&e.");
            configManager.saveConfig();
        } else if (args[0].equalsIgnoreCase("toggle")) {
            configManager.setActivated(!configManager.isActivated());
            PlayerUtils.sendMessageWithPing("&espeed-builder helper has been " + (configManager.isActivated() ? "&aenabled" : "&cdisabled") + "&e.");
            configManager.saveConfig();
        } else if (args[0].equalsIgnoreCase("showtime")) {
            configManager.setStartingMessage(!configManager.isStartingMessage());
            PlayerUtils.sendMessageWithPing("&eShow Times has been " + (configManager.isStartingMessage() ? "&aenabled" : "&cdisabled") + "&e.");
            configManager.saveConfig();
        } else if (args[0].equalsIgnoreCase("setname")) {
            if (args.length < 2) {
                PlayerUtils.sendMessageWithPing("&ePlease enter a username!");
                return;
            }
            configManager.setPlayerName(args[1]);
            PlayerUtils.sendMessageWithPing("&eSet name to &3" + args[1]);
            configManager.saveConfig();
        } else if (args[0].equalsIgnoreCase("times")) {
            List<BuildRecord> times = timeManager.getTimes();
            if (times.isEmpty()) {
                PlayerUtils.sendError("§cNo times recorded yet!");
                return;
            }

            times.sort((a, b) -> Double.compare(a.getBestTime(), b.getBestTime()));

            PlayerUtils.sendLine();
            PlayerUtils.sendMessageWithPing("§6Best Times:");
            for (BuildRecord record : times) {
                String variant = record.getVariant().isEmpty() ? "" : " (" + record.getVariant() + ")";
                PlayerUtils.sendMessage(String.format(" §b%s%s §7(%s): §a%.2fs",
                        record.getTheme(),
                        variant,
                        record.getDifficulty(),
                        record.getBestTime()));
            }
            PlayerUtils.sendLine();
        } else if (args[0].equalsIgnoreCase("overview")) {
            buildTracker.showSessionBestTimesOverview();
        } else if (args[0].equalsIgnoreCase("reset")) {
            buildTracker.clearSessionBestTimes();
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