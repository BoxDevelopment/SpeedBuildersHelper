package box.com;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class CMDS {
    private static final File TIMES_FILE = new File(SpeedBuildersHelper.DIRECTORY, "speedbuilder_times.json");
    private static final Gson GSON = new Gson();
    // Add reference to the SpeedBuildersHelper instance
    private static SpeedBuildersHelper helperInstance;

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            registerCommands(dispatcher);
        });
    }

    // Method to set the helper instance
    public static void setHelperInstance(SpeedBuildersHelper instance) {
        helperInstance = instance;
    }

    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralCommandNode<FabricClientCommandSource> speedbuildersNode = dispatcher.register(
                literal("speedbuilders")
                        .executes(CMDS::showHelp)
                        .then(literal("debug")
                                .executes(CMDS::toggleDebug))
                        .then(literal("toggle")
                                .executes(CMDS::toggleMod))
                        .then(literal("showtime")
                                .executes(CMDS::toggleShowTime))
                        .then(literal("setname")
                                .then(argument("name", StringArgumentType.word())
                                        .executes(ctx -> setName(ctx, StringArgumentType.getString(ctx, "name")))))
                        .then(literal("times")
                                .executes(CMDS::showTimes)
                                .then(argument("theme", StringArgumentType.greedyString())
                                        .executes(ctx -> showTimes(ctx, StringArgumentType.getString(ctx, "theme")))))
                        .then(literal("overview")
                                .executes(CMDS::showOverview))
                        .then(literal("reset")
                                .executes(CMDS::resetSession))
                        .then(literal("debugtest")
                                .executes(CMDS::debugTest))
        );

        dispatcher.register(
                literal("sb")
                        .executes(CMDS::showHelp)  // Add execution handler for bare /sb command
                        .redirect(speedbuildersNode)  // Redirect for subcommands
        );
    }

    private static int showHelp(CommandContext<FabricClientCommandSource> context) {
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
        return 1;
    }
    private static int debugTest(CommandContext<FabricClientCommandSource> context) {
        PlayerUtils.debug("TESTING DEBUG MESSAGE");
        return 1;
    }

    private static int toggleDebug(CommandContext<FabricClientCommandSource> context) {
        SpeedBuildersHelper.Debug = !SpeedBuildersHelper.Debug;
        PlayerUtils.sendMessageWithPing("&eDebug has been " + (SpeedBuildersHelper.Debug ? "&aenabled" : "&cdisabled") + "&e.");
        SpeedBuildersHelper.saveConfig();
        return 1;
    }

    private static int toggleMod(CommandContext<FabricClientCommandSource> context) {
        SpeedBuildersHelper.Activated = !SpeedBuildersHelper.Activated;
        PlayerUtils.sendMessageWithPing("&espeed-builder helper has been " + (SpeedBuildersHelper.Activated ? "&aenabled" : "&cdisabled") + "&e.");
        SpeedBuildersHelper.saveConfig();
        return 1;
    }

    private static int toggleShowTime(CommandContext<FabricClientCommandSource> context) {
        SpeedBuildersHelper.StartingMessage = !SpeedBuildersHelper.StartingMessage;
        PlayerUtils.sendMessageWithPing("&eShow Times has been " + (SpeedBuildersHelper.StartingMessage ? "&aenabled" : "&cdisabled") + "&e.");
        SpeedBuildersHelper.saveConfig();
        return 1;
    }

    private static int setName(CommandContext<FabricClientCommandSource> context, String name) {
        SpeedBuildersHelper.playerName = name;
        PlayerUtils.sendMessageWithPing("&eSet name to &3" + name);
        SpeedBuildersHelper.saveConfig();
        return 1;
    }

    private static int showTimes(CommandContext<FabricClientCommandSource> context) {
        return showTimes(context, "");
    }

    private static int showTimes(CommandContext<FabricClientCommandSource> context, String targetTheme) {
        try {
            if (!TIMES_FILE.exists()) {
                PlayerUtils.sendError("§cNo times recorded yet!");
                return 0;
            }

            JsonObject json = GSON.fromJson(new FileReader(TIMES_FILE), JsonObject.class);
            JsonArray themesArray = json.getAsJsonArray("themes");
            List<Map<String, Object>> times = new ArrayList<>();

            for (JsonElement element : themesArray) {
                JsonObject record = element.getAsJsonObject();
                if (!targetTheme.isEmpty() && !record.get("theme").getAsString().toLowerCase().contains(targetTheme.toLowerCase())) {
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
                return 0;
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
            return 1;
        } catch (Exception e) {
            PlayerUtils.sendError("§cError reading times: " + e.getMessage());
            return 0;
        }
    }

    private static int showOverview(CommandContext<FabricClientCommandSource> context) {
        if (helperInstance != null) {
            helperInstance.showSessionBestTimesOverview();
            return 1;
        } else {
            PlayerUtils.sendError("§cError: Helper instance not available");
            return 0;
        }
    }

    private static int resetSession(CommandContext<FabricClientCommandSource> context) {
        if (helperInstance != null) {
            helperInstance.clearSessionBestTimes();
            return 1;
        } else {
            PlayerUtils.sendError("§cError: Helper instance not available");
            return 0;
        }
    }
}