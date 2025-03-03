package box.com.speedbuilderhelper;



import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import net.minecraft.scoreboard.*;
import net.minecraft.util.ChatComponentText;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PlayerUtils implements MinecraftInstance

{
    public static boolean nullCheck() {
        return mc.thePlayer != null && mc.theWorld != null;
    }

    public static List<String> getSidebarLines() {
        final List<String> lines = new ArrayList<>();
        if (mc.theWorld == null) {
            return lines;
        }
        final Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null) {
            return lines;
        }
        final ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) {
            return lines;
        }
        Collection<Score> scores = scoreboard.getSortedScores(objective);
        final List<Score> list = new ArrayList<>();
        for (final Score input : scores) {
            if (input != null && input.getPlayerName() != null && !input.getPlayerName().startsWith("#")) {
                list.add(input);
            }
        }
        if (list.size() > 15) {
            scores = (Collection<Score>) Lists.newArrayList(Iterables.skip((Iterable)list, scores.size() - 15));
        }
        else {
            scores = list;
        }
        for (final Score score : scores) {
            final ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            lines.add(ScorePlayerTeam.formatPlayerName(team, score.getPlayerName()));
        }
        return lines;
    }

    public static void sendMessage(String message) {
        if (!nullCheck()) {
            return;
        }
        final String txt = replace("§7[§dSpeedBuilders§7]§r " + message);
        mc.thePlayer.addChatMessage(new ChatComponentText(txt));
    }

    public static void sendMessageWithPing(String message) {
        if (!nullCheck()) {
            return;
        }
        final String txt = replace("§7[§dSpeedBuilders§7]§r " + message);
        mc.thePlayer.addChatMessage(new ChatComponentText(txt));
        PlayerUtils.ping();
    }

    public static void sendError(String message) {
        if (!nullCheck()) {
            return;
        }
        final String txt = replace("§7[§dSpeedBuilders§7]§r " + message);
        mc.thePlayer.addChatMessage(new ChatComponentText(txt));
        PlayerUtils.error();
    }


    public static void ping() {
        mc.thePlayer.playSound("note.pling", 1.0f, 1.0f);
    }
    public static void error() {
        mc.thePlayer.playSound("note.pling", 1.0f, 0.5f);
    }

    public static void debug(String message) {
        if (!SpeedBuilderHelper.Debug) {
            return;
        }
        final String txt = replace("&7[&dDEBUG&7]&r " + message);
        mc.thePlayer.addChatMessage(new ChatComponentText(txt));
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
