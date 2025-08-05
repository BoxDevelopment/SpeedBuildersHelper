package box.com.speedbuilderhelper.model;

public class BuildRecord {
    private final String theme;
    private final String difficulty;
    private final String variant;
    private double bestTime;
    private boolean isNewBest;

    public BuildRecord(String theme, String difficulty, String variant, double time) {
        this.theme = theme.replaceAll("§.", "");
        this.difficulty = difficulty.replaceAll("§.", "");
        this.variant = variant != null ? variant.replaceAll("§.", "") : "";
        this.bestTime = time;
        this.isNewBest = false;
    }

    public BuildRecord(String theme, String difficulty, String variant, double time, boolean isNewBest) {
        this.theme = theme.replaceAll("§.", "");
        this.difficulty = difficulty.replaceAll("§.", "");
        this.variant = variant != null ? variant.replaceAll("§.", "") : "";
        this.bestTime = time;
        this.isNewBest = isNewBest;
    }

    public String getTheme() {
        return theme;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getVariant() {
        return variant;
    }

    public double getBestTime() {
        return bestTime;
    }

    public void setBestTime(double bestTime) {
        this.bestTime = bestTime;
    }

    public boolean isNewBest() {
        return isNewBest;
    }

    public void setNewBest(boolean newBest) {
        isNewBest = newBest;
    }
}
