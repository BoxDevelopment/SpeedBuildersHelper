package box.com;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.*;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PlayerUtils implements MinecraftInstance {
    public static boolean nullCheck() {
        return mc.player != null && mc.world != null;
    }

    public static List<String> getSidebarLines() {
        List<String> lines = new ArrayList<>();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) return lines;

        Scoreboard scoreboard = client.world.getScoreboard();
        ScoreboardObjective sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);


        if (sidebar == null) return lines;

        for (String entry : scoreboard.getTeamNames()) {
            lines.add(entry);
        }

        return lines;
    }

    public static void sendMessage(String message) {
        if (!nullCheck()) {
            return;
        }
        final String txt = replace("§7[§dSpeedBuilders§7]§r " + message);
        mc.player.sendMessage(Text.of(txt));
    }

    public static void sendMessageWithPing(String message) {
        if (!nullCheck()) {
            return;
        }
        final String txt = replace("§7[§dSpeedBuilders§7]§r " + message);
        mc.player.sendMessage(Text.of(txt));
        PlayerUtils.ping();
    }

    public static void sendError(String message) {
        if (!nullCheck()) {
            return;
        }
        final String txt = replace("§7[§dSpeedBuilders§7]§r " + message);
        mc.player.sendMessage(Text.of(txt));
        PlayerUtils.error();
    }

    public static void ping() {
        mc.player.playSound(SoundEvent.of(Identifier.of("minecraft:block.note_block.pling")), 1.0f, 1.0f);
    }

    public static void error() {
        mc.player.playSound(SoundEvent.of(Identifier.of("minecraft:block.note_block.pling")), 1.0f, 0.5f);
    }

    public static void debug(String message) {
        if (!SpeedBuildersHelper.Debug) {
            return;
        }
        final String txt = replace("&7[&dDEBUG&7]&r " + message);
        mc.player.sendMessage(Text.of(txt));
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