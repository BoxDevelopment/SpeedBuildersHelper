package box.com.speedbuilderhelper.managers;

import box.com.speedbuilderhelper.utils.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;

import java.util.List;

public class GameManager {

    private String currentTheme = "";
    private String currentDifficulty = "";
    private String currentVariant = "";
    private String lastTrackedTheme = "";
    private String lastTrackedDifficulty = "";
    private String lastTrackedVariant = "";
    private boolean gameOverDisplayed = false;
    private boolean variantDetectionActive = false;
    private long lastVariantScanTime = 0;
    private static final long VARIANT_SCAN_COOLDOWN = 1000;

    public void onTick() {
        List<String> sidebarLines = PlayerUtils.getSidebarLines();

        String oldTheme = currentTheme;
        String oldDifficulty = currentDifficulty;

        for (String line : sidebarLines) {
            if (line.startsWith("Theme: ")) {
                currentTheme = line.substring(7).trim();
            } else if (line.startsWith("Difficulty: ")) {
                currentDifficulty = line.substring(11).trim();
            } else if (line.contains("Game Over!")) {
                if (!gameOverDisplayed) {
                    // Fire game over event
                    gameOverDisplayed = true;
                }
            }
        }

        boolean themeChanged = !currentTheme.equals(oldTheme) && !currentTheme.isEmpty();
        boolean difficultyChanged = !currentDifficulty.equals(oldDifficulty) && !currentDifficulty.isEmpty();

        if (themeChanged) {
            variantDetectionActive = true;
            lastVariantScanTime = System.currentTimeMillis();
            PlayerUtils.debug("Starting variant detection for theme: " + currentTheme);
        }

        if (variantDetectionActive && System.currentTimeMillis() - lastVariantScanTime > VARIANT_SCAN_COOLDOWN) {
            String newVariant = detectVariant(cleanText(currentTheme));
            if (!newVariant.isEmpty()) {
                currentVariant = newVariant;
                variantDetectionActive = false;
                PlayerUtils.debug("Detected variant: " + currentVariant + " for theme: " + currentTheme);
                // Fire theme changed event
                lastTrackedTheme = currentTheme;
                lastTrackedDifficulty = currentDifficulty;
                lastTrackedVariant = currentVariant;
            }

            lastVariantScanTime = System.currentTimeMillis();

            if (System.currentTimeMillis() - lastVariantScanTime > 10000) {
                variantDetectionActive = false;
                PlayerUtils.debug("Stopped variant detection for theme: " + currentTheme);
            }
        }

        if ((themeChanged || difficultyChanged) &&
                (!currentTheme.equals(lastTrackedTheme) ||
                        !currentDifficulty.equals(lastTrackedDifficulty) ||
                        !currentVariant.equals(lastTrackedVariant))) {

            gameOverDisplayed = false;

            if (themeChanged) {
                currentVariant = "";
                PlayerUtils.debug("Theme: " + currentTheme);
                variantDetectionActive = requiresVariantDetection(cleanText(currentTheme));
                if (variantDetectionActive) {
                    lastVariantScanTime = System.currentTimeMillis();
                    PlayerUtils.debug("Starting variant detection for theme: " + currentTheme);
                }
            }
            if (difficultyChanged) {
                PlayerUtils.debug("Difficulty: " + currentDifficulty);
            }

            if (!currentTheme.isEmpty() && !currentDifficulty.isEmpty()) {
                if (!requiresVariantDetection(cleanText(currentTheme)) || !currentVariant.isEmpty()) {
                    // Fire theme changed event
                    lastTrackedTheme = currentTheme;
                    lastTrackedDifficulty = currentDifficulty;
                    lastTrackedVariant = currentVariant;
                } else {
                    // wait for variant detection
                }
            }
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
        return text.replaceAll("§.", "")
                .replaceAll("[^\\u0000-\\u007F]", "")
                .replaceFirst("(?i)Theme: ", "")
                .replaceFirst("(?i)Difficulty: ", "")
                .trim();
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