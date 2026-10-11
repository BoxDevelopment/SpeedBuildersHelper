package box.com;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.scores.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.Collections;
import java.util.List;


public class PlayerUtils implements MinecraftInstance {
    public static boolean nullCheck() {
        return mc.player != null && mc.level != null;
    }

    public static final ObjectArrayList<String> STRING_SCOREBOARD = new ObjectArrayList<>();

    static void updateScoreboard(Minecraft client) {
        try {
            STRING_SCOREBOARD.clear();

            LocalPlayer player = client.player;
            if (player == null || client.level == null) return;

            Scoreboard scoreboard = client.level.getScoreboard();
            Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
            ObjectArrayList<Component> textLines = new ObjectArrayList<>();
            ObjectArrayList<String> stringLines = new ObjectArrayList<>();

            for (ScoreHolder scoreHolder : scoreboard.getTrackedPlayers()) {
                if (scoreboard.listPlayerScores(scoreHolder).containsKey(objective)) {
                    PlayerTeam team = scoreboard.getPlayersTeam(scoreHolder.getScoreboardName());

                    if (team != null) {
                        Component textLine = Component.empty().append(team.getPlayerPrefix().copy()).append(team.getPlayerSuffix().copy());
                        String strLine = team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString();

                        if (!strLine.trim().isEmpty()) {
                            String formatted = ChatFormatting.stripFormatting(strLine);

                            textLines.add(textLine);
                            stringLines.add(formatted);
                        }
                    }
                }
            }

            if (objective != null) {
                stringLines.add(objective.getDisplayName().getString());
                textLines.add(Component.empty().append(objective.getDisplayName().copy()));

                Collections.reverse(stringLines);
                Collections.reverse(textLines);
            }

            STRING_SCOREBOARD.addAll(stringLines);
        } catch (NullPointerException e) {
            //Do nothing
        }
    }

    public static void sendMessage(String message) {
        if (!nullCheck()) {
            return;
        }
        final String txt = replace("§7[§dSpeedBuilders§7]§r " + message);
        mc.player.sendSystemMessage(Component.literal(txt));
    }

    public static void sendMessageWithPing(String message) {
        if (!nullCheck()) {
            return;
        }
        final String txt = replace("§7[§dSpeedBuilders§7]§r " + message);
        mc.player.sendSystemMessage(Component.literal(txt));
        PlayerUtils.ping();
    }

    public static void sendError(String message) {
        if (!nullCheck()) {
            return;
        }
        final String txt = replace("§7[§dSpeedBuilders§7]§r " + message);
        mc.player.sendSystemMessage(Component.literal(txt));
        PlayerUtils.error();
    }

    public static void sendToast(String title, String message) {
        sendToast(title, message, false);
    }

    public static void sendToast(String title, String message, boolean ping) {
        Component toastTitle = Component.literal(stripFormatting(title));
        Component toastMessage = Component.literal(stripFormatting(message));
        BuildInfoToast.show(toastTitle, toastMessage);

        if (ping && nullCheck()) {
            PlayerUtils.ping();
        }
    }

    private static String stripFormatting(String text) {
        if (text == null) {
            return "";
        }

        String translated = replace(text);
        String stripped = ChatFormatting.stripFormatting(translated);
        return stripped != null ? stripped : translated;
    }

    public static void ping() {
        mc.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f, 1.0f);
    }

    public static void error() {
        mc.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f, 0.5f);
    }

    public static void debug(String message) {
        if (!SpeedBuildersHelper.Debug) {
            return;
        }
        final String txt = replace("&7[&dDEBUG&7]&r " + message);
        mc.player.sendSystemMessage(Component.literal(txt));
    }

    public static void sendLine() {
        sendMessage("&7&m-------------------------");
    }

    public static String replace(String text) {
        return text.replace("&", "§").replace("%and", "&");
    }

    public static double round(double number, int decimals) {
        if (decimals == 0) {
            return Math.round(number);
        }
        double power = Math.pow(10.0, decimals);
        return (double)Math.round(number * power) / power;
    }

    public static boolean contains(List<String> list, String target) {
        for (String string : list) {
            if (string.equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }
}