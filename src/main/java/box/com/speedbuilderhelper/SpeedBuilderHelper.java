package box.com.speedbuilderhelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.List;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.util.BlockPos;

@Mod(modid = SpeedBuilderHelper.MODID, version = SpeedBuilderHelper.VERSION)
public class SpeedBuilderHelper {

    public static final String MODID = "speedbuilderhelper";
    public static final String VERSION = "1.1";

    public static final Logger LOG = LogManager.getLogger("SPEEDBUILDERS");
    public final static File Directory = new File(Minecraft.getMinecraft().mcDataDir + File.separator + "SpeedBuildersHelper");
    private static final File TIMES_FILE = new File(Directory,"speedbuilder_times.json");
    private static final File CONFIG_FILE = new File(Directory,"speedbuildershelper.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static String playerName;
    private List<BuildRecord> times = new ArrayList<>();
    private List<BuildRecord> sessionTimes = new ArrayList<>();
    private List<BuildRecord> sessionBestTimes = new ArrayList<>();

    private String currentTheme = "";
    private String currentDifficulty = "";
    private String currentVariant = "";
    private String lastTrackedTheme = "";
    private String lastTrackedDifficulty = "";
    private String lastTrackedVariant = "";
    public static boolean Debug = false;
    private boolean gameOverDisplayed = false;
    public static boolean Activated = false;
    public static boolean StartingMessage = false;
    private boolean variantDetectionActive = false;
    private long lastVariantScanTime = 0;
    private static final long VARIANT_SCAN_COOLDOWN = 1000;

    public static class BuildRecord {
        String theme;
        String difficulty;
        String variant;
        double bestTime;
        boolean isNewBest;

        BuildRecord(String theme, String difficulty, String variant, double time) {
            this.theme = theme.replaceAll("§.", "");
            this.difficulty = difficulty.replaceAll("§.", "");
            this.variant = variant != null ? variant.replaceAll("§.", "") : "";
            this.bestTime = time;
            this.isNewBest = false;
        }

        BuildRecord(String theme, String difficulty, String variant, double time, boolean isNewBest) {
            this.theme = theme.replaceAll("§.", "");
            this.difficulty = difficulty.replaceAll("§.", "");
            this.variant = variant != null ? variant.replaceAll("§.", "") : "";
            this.bestTime = time;
            this.isNewBest = isNewBest;
        }
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Directory.exists()) {
            Directory.mkdirs();
        }
        ClientCommandHandler.instance.registerCommand(new CMDS());
        MinecraftForge.EVENT_BUS.register(this);
        loadTimes();
        loadConfig();
    }

    public void clearSessionBestTimes() {
        sessionBestTimes.clear();
        PlayerUtils.sendMessageWithPing("§eSession best times have been reset.");
    }

    public List<BuildRecord> getSessionBestTimes() {
        return sessionBestTimes;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null || !Activated) return;

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
                    showSessionOverview();
                    sessionTimes.clear();
                    gameOverDisplayed = true;
                }
            }
        }

        boolean themeChanged = !currentTheme.equals(oldTheme) && !currentTheme.isEmpty();
        boolean difficultyChanged = !currentDifficulty.equals(oldDifficulty) && !currentDifficulty.isEmpty();

        if (themeChanged && !currentTheme.isEmpty()) {
            variantDetectionActive = true;
            lastVariantScanTime = System.currentTimeMillis();
            PlayerUtils.debug("Starting variant detection for theme: " + currentTheme);
        }

        if (variantDetectionActive &&
                System.currentTimeMillis() - lastVariantScanTime > VARIANT_SCAN_COOLDOWN) {

            String newVariant = detectVariant(cleanText(currentTheme));
            if (!newVariant.isEmpty()) {
                currentVariant = newVariant;
                variantDetectionActive = false;
                PlayerUtils.debug("Detected variant: " + currentVariant + " for theme: " + currentTheme);
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
                variantDetectionActive = requiresVariantDetection(cleanText(currentTheme));  // Only enable detection if needed
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
                    showBestTime(currentTheme, currentDifficulty, currentVariant);

                    lastTrackedTheme = currentTheme;
                    lastTrackedDifficulty = currentDifficulty;
                    lastTrackedVariant = currentVariant;
                }
            }
        }
    }

    private boolean requiresVariantDetection(String theme) {
        return theme.equalsIgnoreCase("Painting") ||
                theme.equalsIgnoreCase("ClownFish");
    }

    private String detectVariant(String theme) {
        if (theme.equalsIgnoreCase("Painting")) {
            return detectPaintingVariant();
        }
        else if (theme.equalsIgnoreCase("ClownFish")) {
            return detectClownFishVariant();
        }

        return "";
    }

    private String detectPaintingVariant() {
        boolean hasSpruce = checkForBlockInBuildArea("planks");

        return hasSpruce ? "Horizontal" : "Vertical";
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

    private void showBestTime(String theme, String difficulty, String variant) {
        if (!StartingMessage) {
            return;
        }
        String cleanTheme = cleanText(theme);
        String cleanDifficulty = cleanText(difficulty);

        for (BuildRecord record : times) {
            if (record.theme.equalsIgnoreCase(cleanTheme) &&
                    record.difficulty.equalsIgnoreCase(cleanDifficulty) &&
                    record.variant.equals(variant)) {

                String variantDisplay = variant.isEmpty() ? "" : " (" + variant + ")";
                PlayerUtils.sendMessage("§b" + cleanTheme + variantDisplay + " §7(" + cleanDifficulty + ") §eBest Time: §a" +
                        PlayerUtils.round(record.bestTime, 2) + "s");
                return;
            }
        }

        String variantDisplay = variant.isEmpty() ? "" : " (" + variant + ")";
        PlayerUtils.sendMessage("§b" + cleanTheme + variantDisplay + " §7(" + cleanDifficulty + ") §eNo previous record");
    }

    private String cleanText(String text) {
        return text.replaceAll("§.", "")
                .replaceAll("[^\\x00-\\x7F]", "")
                .replaceFirst("(?i)Theme: ", "")
                .replaceFirst("(?i)Difficulty: ", "")
                .trim();
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!Activated) {
            return;
        }
        String message = event.message.getUnformattedText();

        Pattern pattern = Pattern.compile("(.*) got a perfect build in (.*)s!");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find() && matcher.group(1).equals(playerName)) {
            double time = Double.parseDouble(matcher.group(2));
            boolean isNewBestTime = updateTime(currentTheme, currentDifficulty, currentVariant, time);

            sessionTimes.add(new BuildRecord(cleanText(currentTheme), cleanText(currentDifficulty), currentVariant, time));

            if (isNewBestTime) {
                updateSessionBestTimes(cleanText(currentTheme), cleanText(currentDifficulty), currentVariant, time);
            }
        }
    }

    private boolean updateTime(String theme, String difficulty, String variant, double time) {
        String cleanTheme = cleanText(theme);
        String cleanDifficulty = cleanText(difficulty);
        boolean isNewBest = false;

        for (BuildRecord record : times) {
            if (record.theme.equals(cleanTheme) &&
                    record.difficulty.equals(cleanDifficulty) &&
                    record.variant.equals(variant)) {

                if (time < record.bestTime) {
                    record.bestTime = time;
                    saveTimes();

                    String variantDisplay = variant.isEmpty() ? "" : " (" + variant + ")";
                    PlayerUtils.sendMessage("§a§lNew Best Time! " + cleanTheme + variantDisplay +
                            " (" + cleanDifficulty + "): §b" + time + "§7s");
                    isNewBest = true;
                } else {
                    String variantDisplay = variant.isEmpty() ? "" : " (" + variant + ")";
                    PlayerUtils.sendMessage("§aCompleted " + cleanTheme + variantDisplay +
                            " (" + cleanDifficulty + "): §b" + time + "§7s, Previous: " +
                            record.bestTime + "s");
                }
                return isNewBest;
            }
        }

        times.add(new BuildRecord(cleanTheme, cleanDifficulty, variant, time));
        saveTimes();

        String variantDisplay = variant.isEmpty() ? "" : " (" + variant + ")";
        PlayerUtils.sendMessage("§aFirst completion! §b" + cleanTheme + variantDisplay +
                " (" + cleanDifficulty + ")§a in §b" + time + "§7s!");
        isNewBest = true;
        return isNewBest;
    }

    private void updateSessionBestTimes(String theme, String difficulty, String variant, double time) {
        for (BuildRecord record : sessionBestTimes) {
            if (record.theme.equalsIgnoreCase(theme) &&
                    record.difficulty.equalsIgnoreCase(difficulty) &&
                    record.variant.equals(variant)) {

                if (time < record.bestTime) {
                    record.bestTime = time;
                }
                return;
            }
        }

        BuildRecord newRecord = new BuildRecord(theme, difficulty, variant, time, true);
        sessionBestTimes.add(newRecord);
    }

    private void showSessionOverview() {
        if (sessionTimes.isEmpty()) {
            PlayerUtils.sendMessage("§cNo builds completed this game.");
            return;
        }

        PlayerUtils.sendMessage("§6=== Game Overview ===");
        for (BuildRecord record : sessionTimes) {
            double bestTime = getBestTime(record.theme, record.difficulty, record.variant);
            boolean isNewBest = record.bestTime <= bestTime;

            String variantDisplay = record.variant.isEmpty() ? "" : " (" + record.variant + ")";
            String timeMessage = "§b" + record.theme + variantDisplay + " §7(" + record.difficulty + ") §eTime: §a" +
                    PlayerUtils.round(record.bestTime, 2) + "s";

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

        sessionBestTimes.sort((a, b) -> Double.compare(a.bestTime, b.bestTime));

        for (BuildRecord record : sessionBestTimes) {
            String variantDisplay = record.variant.isEmpty() ? "" : " (" + record.variant + ")";
            PlayerUtils.sendMessage("§b" + record.theme + variantDisplay + " §7(" + record.difficulty + ") §eBest: §a" +
                    PlayerUtils.round(record.bestTime, 2) + "s");
        }
        PlayerUtils.sendLine();
    }

    private double getBestTime(String theme, String difficulty, String variant) {
        for (BuildRecord record : times) {
            if (record.theme.equalsIgnoreCase(theme) &&
                    record.difficulty.equalsIgnoreCase(difficulty) &&
                    record.variant.equals(variant)) {

                return record.bestTime;
            }
        }
        return Double.MAX_VALUE;
    }

    private void loadTimes() {
        if (!TIMES_FILE.exists()) {
            try {
                TIMES_FILE.createNewFile();
                saveTimes();
                LOG.info("Created Times File " + TIMES_FILE.getAbsolutePath());
            } catch (IOException e) {
                LOG.error("Failed to create times file: " + e.getMessage());
                e.printStackTrace();
            }
            return;
        }

        try (Reader reader = new FileReader(TIMES_FILE)) {
            Type type = new TypeToken<HashMap<String, List<BuildRecord>>>(){}.getType();
            Map<String, List<BuildRecord>> data = GSON.fromJson(reader, type);
            if (data != null && data.containsKey("themes")) {
                times = data.get("themes");

                // Handle migration of old data if variant field is missing
                migrateOldRecords();
            }
        } catch (IOException e) {
            LOG.error("Failed to read times file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void migrateOldRecords() {
        boolean needsMigration = false;

        for (BuildRecord record : times) {
            if (record.variant == null) {
                needsMigration = true;
                break;
            }
        }

        if (needsMigration) {
            LOG.info("Migrating old records to new format with variant field");
            List<BuildRecord> newRecords = new ArrayList<>();

            for (BuildRecord oldRecord : times) {
                if (oldRecord.variant == null) {
                    BuildRecord newRecord = new BuildRecord(
                            oldRecord.theme,
                            oldRecord.difficulty,
                            "",
                            oldRecord.bestTime,
                            oldRecord.isNewBest
                    );
                    newRecords.add(newRecord);
                } else {
                    newRecords.add(oldRecord);
                }
            }

            times = newRecords;
            saveTimes();
            LOG.info("Migration completed successfully");
        }
    }

    private void saveTimes() {
        try (Writer writer = new FileWriter(TIMES_FILE)) {
            Map<String, List<BuildRecord>> data = new HashMap<>();
            data.put("themes", times);
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadConfig() {
        if (!CONFIG_FILE.exists()) {
            Activated = true;
            StartingMessage = true;
            Debug = false;
            playerName = "";
            saveConfig();
            LOG.info("Created Config File " + CONFIG_FILE.getAbsolutePath());
            return;
        }

        try (Reader reader = new FileReader(CONFIG_FILE)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);

            if (json != null) {
                Activated = json.has("activated") ? json.get("activated").getAsBoolean() : true;
                StartingMessage = json.has("startingMessage") ? json.get("startingMessage").getAsBoolean() : true;
                Debug = json.has("debug") ? json.get("debug").getAsBoolean() : false;
                playerName = json.has("playerName") ? json.get("playerName").getAsString() : "";

                LOG.info("Loaded config: activated=" + Activated +
                        ", startingMessage=" + StartingMessage +
                        ", debug=" + Debug +
                        ", playerName=" + playerName);
            }
        } catch (IOException e) {
            LOG.error("Failed to read config file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void saveConfig() {
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            JsonObject json = new JsonObject();
            json.addProperty("activated", Activated);
            json.addProperty("startingMessage", StartingMessage);
            json.addProperty("debug", Debug);
            json.addProperty("playerName", playerName != null ? playerName : "");

            GSON.toJson(json, writer);
            LOG.info("Saved config: activated=" + Activated +
                    ", startingMessage=" + StartingMessage +
                    ", debug=" + Debug +
                    ", playerName=" + playerName);
        } catch (IOException e) {
            LOG.error("Failed to save config file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}