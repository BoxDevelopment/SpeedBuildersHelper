package box.com.speedbuilderhelper;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class CMDS extends CommandBase {
    private static final File TIMES_FILE = new File(SpeedBuilderHelper.Directory,"speedbuilder_times.json");
    private static final Gson GSON = new Gson();

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
        return Arrays.asList("sb", "speedbuilders");
    }


    @Override
    public void
    processCommand(ICommandSender sender, String[] args) {
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
        }
        else if (args[0].equalsIgnoreCase("debug")) {
            SpeedBuilderHelper.Debug = !SpeedBuilderHelper.Debug;
            PlayerUtils.sendMessageWithPing("&eDebug has been " + (SpeedBuilderHelper.Debug ? "&aenabled" : "&cdisabled") + "&e.");
            SpeedBuilderHelper.saveConfig();
        }
        else if (args[0].equalsIgnoreCase("toggle")) {
            SpeedBuilderHelper.Activated = !SpeedBuilderHelper.Activated;
            PlayerUtils.sendMessageWithPing("&espeed-builder helper has been " + (SpeedBuilderHelper.Activated ? "&aenabled" : "&cdisabled") + "&e.");
            SpeedBuilderHelper.saveConfig();
        }
        else if (args[0].equalsIgnoreCase("showtime")) {
            SpeedBuilderHelper.StartingMessage = !SpeedBuilderHelper.StartingMessage;
            PlayerUtils.sendMessageWithPing("&eShow Times has been " + (SpeedBuilderHelper.StartingMessage ? "&aenabled" : "&cdisabled") + "&e.");
            SpeedBuilderHelper.saveConfig();
        }
        else if (args[0].equalsIgnoreCase("setname")) {
            if (args.length < 2) {
                PlayerUtils.sendMessageWithPing("&ePlease enter a username!");
                return;
            }
            SpeedBuilderHelper.playerName = args[1];
            PlayerUtils.sendMessageWithPing("&eSet name to &3" + args[1]);
            SpeedBuilderHelper.saveConfig();
        }
        else if (args[0].equalsIgnoreCase("times")) {
            try {
                if (!TIMES_FILE.exists()) {
                    PlayerUtils.sendError("§cNo times recorded yet!");
                    return;
                }

                JsonObject json = GSON.fromJson(new FileReader(TIMES_FILE), JsonObject.class);
                JsonArray themesArray = json.getAsJsonArray("themes");
                List<Map<String, Object>> times = new ArrayList<>();

                String targetTheme = args.length > 1 ? args[1].toLowerCase() : "";

                for (JsonElement element : themesArray) {
                    JsonObject record = element.getAsJsonObject();
                    if (!targetTheme.isEmpty() && !record.get("theme").getAsString().toLowerCase().contains(targetTheme)) {
                        continue;
                    }
                    Map<String, Object> timeRecord = new HashMap<>();
                    timeRecord.put("theme", record.get("theme").getAsString());
                    timeRecord.put("difficulty", record.get("difficulty").getAsString());
                    timeRecord.put("bestTime", record.get("bestTime").getAsDouble());
                    if (record.has("variant") && !record.get("variant").getAsString().isEmpty()) {
                        timeRecord.put("variant", record.get("variant").getAsString());
                    }
                    times.add(timeRecord);
                }

                if (times.isEmpty()) {
                    PlayerUtils.sendError("§cNo times found" + (targetTheme.isEmpty() ? "" : " for theme: " + targetTheme));
                    return;
                }

                times.sort((a, b) -> Double.compare((double) a.get("bestTime"), (double) b.get("bestTime")));

                PlayerUtils.sendLine();
                PlayerUtils.sendMessageWithPing("§6Best Times" + (targetTheme.isEmpty() ? "" : " for " + targetTheme) + ":");
                for (Map<String, Object> record : times) {
                    String theme = (String) record.get("theme");
                    String difficulty = (String) record.get("difficulty");
                    double bestTime = (double) record.get("bestTime");

                    String variant = record.containsKey("variant") ? " (" + record.get("variant") + ")" : "";

                    PlayerUtils.sendMessage(String.format(" §b%s%s §7(%s): §a%.2fs",
                            theme,
                            variant,
                            difficulty,
                            bestTime));
                }
                PlayerUtils.sendLine();
            } catch (Exception e) {
                PlayerUtils.sendError("§cError reading times: " + e.getMessage());
            }
        }
        else if (args[0].equalsIgnoreCase("overview")) {
            SpeedBuilderHelper instance = (SpeedBuilderHelper) net.minecraftforge.fml.common.Loader.instance()
                    .getIndexedModList().get(SpeedBuilderHelper.MODID).getMod();

            instance.showSessionBestTimesOverview();
        }
        else if (args[0].equalsIgnoreCase("reset")) {
            SpeedBuilderHelper instance = (SpeedBuilderHelper) net.minecraftforge.fml.common.Loader.instance()
                    .getIndexedModList().get(SpeedBuilderHelper.MODID).getMod();

            instance.clearSessionBestTimes();
        }
        else {
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