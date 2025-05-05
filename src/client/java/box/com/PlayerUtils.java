package box.com;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.*;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class PlayerUtils implements MinecraftInstance {
    public static boolean nullCheck() {
        return mc.player != null && mc.world != null;
    }

    public static final ObjectArrayList<String> STRING_SCOREBOARD = new ObjectArrayList<>();

    static void updateScoreboard(MinecraftClient client) {
        try {
            STRING_SCOREBOARD.clear();

            ClientPlayerEntity player = client.player;
            if (player == null) return;

            Scoreboard scoreboard = player.getScoreboard();
            ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.FROM_ID.apply(1));
            ObjectArrayList<Text> textLines = new ObjectArrayList<>();
            ObjectArrayList<String> stringLines = new ObjectArrayList<>();

            for (ScoreHolder scoreHolder : scoreboard.getKnownScoreHolders()) {
                if (scoreboard.getScoreHolderObjectives(scoreHolder).containsKey(objective)) {
                    Team team = scoreboard.getScoreHolderTeam(scoreHolder.getNameForScoreboard());

                    if (team != null) {
                        Text textLine = Text.empty().append(team.getPrefix().copy()).append(team.getSuffix().copy());
                        String strLine = team.getPrefix().getString() + team.getSuffix().getString();

                        if (!strLine.trim().isEmpty()) {
                            String formatted = Formatting.strip(strLine);

                            textLines.add(textLine);
                            stringLines.add(formatted);
                        }
                    }
                }
            }

            if (objective != null) {
                stringLines.add(objective.getDisplayName().getString());
                textLines.add(Text.empty().append(objective.getDisplayName().copy()));

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
        mc.player.sendMessage(Text.of(txt),false);
    }

    public static void sendMessageWithPing(String message) {
        if (!nullCheck()) {
            return;
        }
        final String txt = replace("§7[§dSpeedBuilders§7]§r " + message);
        mc.player.sendMessage(Text.of(txt),false);
        PlayerUtils.ping();
    }

    public static void sendError(String message) {
        if (!nullCheck()) {
            return;
        }
        final String txt = replace("§7[§dSpeedBuilders§7]§r " + message);
        mc.player.sendMessage(Text.of(txt),false);
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
        mc.player.sendMessage(Text.literal(txt),false);
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