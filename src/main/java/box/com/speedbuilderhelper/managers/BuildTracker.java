package box.com.speedbuilderhelper.managers;

import box.com.speedbuilderhelper.model.BuildRecord;
import box.com.speedbuilderhelper.utils.PlayerUtils;

import java.util.ArrayList;
import java.util.List;

public class BuildTracker {

    private final List<BuildRecord> sessionTimes = new ArrayList<>();
    private final List<BuildRecord> sessionBestTimes = new ArrayList<>();
    private final TimeManager timeManager;

    public BuildTracker(TimeManager timeManager) {
        this.timeManager = timeManager;
    }

    public void onBuildComplete(String theme, String difficulty, String variant, double time) {
        boolean isNewBestTime = timeManager.updateTime(theme, difficulty, variant, time);

        sessionTimes.add(new BuildRecord(theme, difficulty, variant, time));

        if (isNewBestTime) {
            updateSessionBestTimes(theme, difficulty, variant, time);
        }
    }

    private void updateSessionBestTimes(String theme, String difficulty, String variant, double time) {
        for (BuildRecord record : sessionBestTimes) {
            if (record.getTheme().equalsIgnoreCase(theme) &&
                    record.getDifficulty().equalsIgnoreCase(difficulty) &&
                    record.getVariant().equals(variant)) {

                if (time < record.getBestTime()) {
                    record.setBestTime(time);
                }
                return;
            }
        }

        sessionBestTimes.add(new BuildRecord(theme, difficulty, variant, time, true));
    }

    public void showSessionOverview() {
        if (sessionTimes.isEmpty()) {
            PlayerUtils.sendMessage("§cNo builds completed this game.");
            return;
        }

        PlayerUtils.sendMessage("§6=== Game Overview ===");
        for (BuildRecord record : sessionTimes) {
            double bestTime = timeManager.getBestTime(record.getTheme(), record.getDifficulty(), record.getVariant());
            boolean isNewBest = record.getBestTime() <= bestTime;

            String variantDisplay = record.getVariant().isEmpty() ? "" : " (" + record.getVariant() + ")";
            String timeMessage = "§b" + record.getTheme() + variantDisplay + " §7(" + record.getDifficulty() + ") §eTime: §a" +
                    PlayerUtils.round(record.getBestTime(), 2) + "s";

            if (isNewBest) {
                timeMessage += " §6(New Best!)";
            } else {
                timeMessage += " §7[Best: " + PlayerUtils.round(bestTime, 2) + "s]";
            }

            PlayerUtils.sendMessage(timeMessage);
        }
    }

    public void showSessionBestTimesOverview() {
        if (sessionBestTimes.isEmpty()) {
            PlayerUtils.sendMessage("§cNo new best times recorded this session.");
            return;
        }

        PlayerUtils.sendLine();
        PlayerUtils.sendMessageWithPing("§6=== Session Best Times ===");

        sessionBestTimes.sort((a, b) -> Double.compare(a.getBestTime(), b.getBestTime()));

        for (BuildRecord record : sessionBestTimes) {
            String variantDisplay = record.getVariant().isEmpty() ? "" : " (" + record.getVariant() + ")";
            PlayerUtils.sendMessage("§b" + record.getTheme() + variantDisplay + " §7(" + record.getDifficulty() + ") §eBest: §a" +
                    PlayerUtils.round(record.getBestTime(), 2) + "s");
        }
        PlayerUtils.sendLine();
    }

    public void clearSessionBestTimes() {
        sessionBestTimes.clear();
        PlayerUtils.sendMessageWithPing("§eSession best times have been reset.");
    }

    public void clearSessionTimes() {
        sessionTimes.clear();
    }
}