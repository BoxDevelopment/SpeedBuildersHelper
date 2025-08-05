package box.com.speedbuilderhelper.managers;

import box.com.speedbuilderhelper.utils.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;

import java.util.List;

public class GameManager {

    private final TimeManager timeManager;
    private String currentTheme = "";
    private String currentDifficulty = "";
    private String currentVariant = "";
    private boolean variantDetectionActive = false;
    private long lastVariantScanTime = 0;
    private static final long VARIANT_SCAN_COOLDOWN = 1000;

    public GameManager(TimeManager timeManager) {
        this.timeManager = timeManager;
    }

    public void onTick() {
        List<String> sidebarLines = PlayerUtils.getSidebarLines();

        String newTheme = "";
        String newDifficulty = "";

        for (String line : sidebarLines) {
            if (line.startsWith("Theme: ")) {
                newTheme = cleanText(line.substring(7));
            } else if (line.startsWith("Difficulty: ")) {
                newDifficulty = cleanText(line.substring(11));
            }
        }

        if (!newTheme.isEmpty() && !newTheme.equals(currentTheme)) {
            currentTheme = newTheme;
            currentVariant = "";
            PlayerUtils.debug("Theme changed to: " + currentTheme);

            if (requiresVariantDetection(currentTheme)) {
                variantDetectionActive = true;
                lastVariantScanTime = System.currentTimeMillis();
                PlayerUtils.debug("Starting variant detection for theme: " + currentTheme);
            } else {
                variantDetectionActive = false;
                displayBestTime();
            }
        }

        if (!newDifficulty.isEmpty() && !newDifficulty.equals(currentDifficulty)) {
            currentDifficulty = newDifficulty;
            PlayerUtils.debug("Difficulty changed to: " + currentDifficulty);
            if (!variantDetectionActive) {
                displayBestTime();
            }
        }

        if (variantDetectionActive && System.currentTimeMillis() - lastVariantScanTime > VARIANT_SCAN_COOLDOWN) {
            String detectedVariant = detectVariant(currentTheme);
            if (!detectedVariant.isEmpty()) {
                currentVariant = detectedVariant;
                variantDetectionActive = false;
                PlayerUtils.debug("Detected variant: " + currentVariant + " for theme: " + currentTheme);
                displayBestTime();
            }
            lastVariantScanTime = System.currentTimeMillis();
        }
    }

    private void displayBestTime() {
        if (currentTheme.isEmpty() || currentDifficulty.isEmpty()) return;

        double bestTime = timeManager.getBestTime(currentTheme, currentDifficulty, currentVariant);
        if (bestTime != Double.MAX_VALUE) {
            String variantDisplay = currentVariant.isEmpty() ? "" : " (" + currentVariant + ")";
            PlayerUtils.sendMessage("§b" + currentTheme + variantDisplay + " §7(" + currentDifficulty + ") §eBest Time: §a" + PlayerUtils.round(bestTime, 2) + "s");
        } else {
            PlayerUtils.sendMessage("§b" + currentTheme + " §7(" + currentDifficulty + ") - §eNo best time recorded.");
        }
    }

    private boolean requiresVariantDetection(String theme) {
        return theme.equalsIgnoreCase("Painting") || theme.equalsIgnoreCase("ClownFish");
    }

    private String detectVariant(String theme) {
        if (theme.equalsIgnoreCase("Painting")) {
            return detectPaintingVariant();
        } else if (theme.equalsIgnoreCase("ClownFish")) {
            return detectClownFishVariant();
        }
        return "";
    }

    private String detectPaintingVariant() {
        boolean hasSpruce = checkForBlockInBuildArea("planks");
        return hasSpruce ? "Vertical" : "Horizontal";
    }

    private String detectClownFishVariant() {
        int blackWoolCount = countBlocksInBuildArea("wool");
        return blackWoolCount == 1 ? "Small" : "Medium";
    }

    private boolean checkForBlockInBuildArea(String blockId) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return false;

        int px = (int) mc.thePlayer.posX;
        int py = (int) mc.thePlayer.posY;
        int pz = (int) mc.thePlayer.posZ;

        int radius = 15;

        for (int x = px - radius; x <= px + radius; x++) {
            for (int y = py - radius; y <= py + radius; y++) {
                for (int z = pz - radius; z <= pz + radius; z++) {
                    if (mc.theWorld.getBlockState(new BlockPos(x, y, z))
                            .getBlock().getUnlocalizedName().toLowerCase().contains(blockId.toLowerCase())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private int countBlocksInBuildArea(String blockId) {
        int count = 0;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return count;

        int px = (int) mc.thePlayer.posX;
        int py = (int) mc.thePlayer.posY;
        int pz = (int) mc.thePlayer.posZ;

        int radius = 15;

        for (int x = px - radius; x <= px + radius; x++) {
            for (int y = py - radius; y <= py + radius; y++) {
                for (int z = pz - radius; z <= pz + radius; z++) {
                    if (mc.theWorld.getBlockState(new BlockPos(x, y, z))
                            .getBlock().getUnlocalizedName().toLowerCase().contains(blockId.toLowerCase())) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    private String cleanText(String text) {
        return text.replaceAll("§[0-9a-fk-or]", "").trim();
    }


    public String getCurrentTheme() {
        return currentTheme;
    }

    public String getCurrentDifficulty() {
        return currentDifficulty;
    }

    public String getCurrentVariant() {
        return currentVariant;
    }
}
