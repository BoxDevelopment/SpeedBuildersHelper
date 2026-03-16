package box.com;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpeedBuildersHelper implements ClientModInitializer {

	public static final String MODID = "speedbuildershelper";
	public static final String VERSION = "1.1";
	private static final String VERSION_CHECK_URL = "https://raw.githubusercontent.com/BoxDevelopment/SpeedBuildersHelper/1.21/version.txt";
	private static final HttpClient VERSION_HTTP_CLIENT = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.build();

	public static final Logger LOG = LogManager.getLogger("SPEEDBUILDERS");
	public static File DIRECTORY = new File(MinecraftClient.getInstance().runDirectory, "SpeedBuildersHelper");
	private static final File TIMES_FILE = new File(DIRECTORY, "speedbuilder_times.json");
	private static final File CONFIG_FILE = new File(DIRECTORY, "speedbuildershelper.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	public static String playerName;
	private List<BuildRecord> times = new ArrayList<>();
	private List<BuildRecord> sessionTimes = new ArrayList<>();
	private List<BuildRecord> sessionBestTimes = new ArrayList<>();

	public List<BlockPos> platformPositions = Arrays.asList(
			new BlockPos(-15, 72, 45),
			new BlockPos(18, 72, 45),
			new BlockPos(45, 72, 16),
			new BlockPos(45, 72, -17),
			new BlockPos(16, 72, -44),
			new BlockPos(-17, 72, -44),
			new BlockPos(-44, 72, -15),
			new BlockPos(-44, 72, 18)
	);

	private String currentTheme = "";
	private String currentDifficulty = "";
	private String currentVariant = "";
	private String lastTrackedTheme = "";
	private String lastTrackedDifficulty = "";
	private String lastTrackedVariant = "";
	private BlockPos closestPlatform = null;
	private int lastGameState = -1;

	public static boolean Debug = false;
	private boolean gameOverDisplayed = false;
	public static boolean Activated = false;
	public static boolean StartingMessage = false;
	private volatile boolean versionCheckFinished = false;
	private volatile boolean outdatedVersionDetected = false;
	private volatile boolean versionWarningSent = false;
	private volatile String latestRemoteVersion = "";
	private String localModVersion = VERSION;

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

	@Override
	public void onInitializeClient() {
		if (!DIRECTORY.exists()) {
			DIRECTORY.mkdirs();
		}
		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
		ClientReceiveMessageEvents.GAME.register(this::onChat);
		CMDS.setHelperInstance(this);
		CMDS.register();

		loadTimes();
		loadConfig();
		startVersionCheck();
	}

	public void clearSessionBestTimes() {
		sessionBestTimes.clear();
		PlayerUtils.sendMessageWithPing("§eSession best times have been reset.");
	}

	public List<BuildRecord> getSessionBestTimes() {
		return sessionBestTimes;
	}

	private void onClientTick(MinecraftClient client) {
		handleVersionWarning(client);

		if (client.world == null || client.player == null || !Activated) return;
		PlayerUtils.updateScoreboard(MinecraftInstance.mc);

		String oldTheme = currentTheme;
		String oldDifficulty = currentDifficulty;

		int gameState = getGameState();

		// Detect platform once when round starts
		if (gameState == 2 && lastGameState != 2) {
			detectClosestPlatform();
		}

		// Clear platform when round ends
		if (gameState != 2 && lastGameState == 2) {
			closestPlatform = null;
		}

		for (String line : PlayerUtils.STRING_SCOREBOARD) {
			if (line.toLowerCase().contains("theme:")) {
				String extractedTheme = line.substring(line.toLowerCase().indexOf("theme:") + 6).trim();
				currentTheme = extractedTheme;
				//PlayerUtils.debug("Found theme: '" + currentTheme + "' from line: '" + line + "'");
			} else if (line.toLowerCase().contains("difficulty:")) {
				String extractedDifficulty = line.substring(line.toLowerCase().indexOf("difficulty:") + 11).trim();
				currentDifficulty = extractedDifficulty;
				//PlayerUtils.debug("Found difficulty: '" + currentDifficulty + "' from line: '" + line + "'");
			} else if (line.contains("Game Over!")) {
				PlayerUtils.debug("Game over detected in scoreboard");
				if (!gameOverDisplayed) {
					showSessionOverview();
					sessionTimes.clear();
					gameOverDisplayed = true;
				}
			}
		}

		boolean themeChanged = !currentTheme.equals(oldTheme) && !currentTheme.isEmpty();
		boolean difficultyChanged = !currentDifficulty.equals(oldDifficulty) && !currentDifficulty.isEmpty();

		if (themeChanged) {
			//PlayerUtils.debug("THEME CHANGED from '" + oldTheme + "' to '" + currentTheme + "'");

			// Detect variant immediately when theme changes
			if (gameState == 2) {
				currentVariant = getVariant();
				//PlayerUtils.debug("Detected variant: '" + currentVariant + "' for theme: " + currentTheme);

				if (!currentTheme.isEmpty() && !currentDifficulty.isEmpty()) {
					showBestTime(currentTheme, currentDifficulty, currentVariant);
					lastTrackedTheme = currentTheme;
					lastTrackedDifficulty = currentDifficulty;
					lastTrackedVariant = currentVariant;
				}
			}
		}

		if (difficultyChanged) {
			//PlayerUtils.debug("DIFFICULTY CHANGED from '" + oldDifficulty + "' to '" + currentDifficulty + "'");
		}

		if ((themeChanged || difficultyChanged) &&
				(!currentTheme.equals(lastTrackedTheme) ||
						!currentDifficulty.equals(lastTrackedDifficulty) ||
						!currentVariant.equals(lastTrackedVariant))) {

			gameOverDisplayed = false;

			if (!currentTheme.isEmpty() && !currentDifficulty.isEmpty()) {
				showBestTime(currentTheme, currentDifficulty, currentVariant);
				lastTrackedTheme = currentTheme;
				lastTrackedDifficulty = currentDifficulty;
				lastTrackedVariant = currentVariant;
			}
		}

		lastGameState = gameState;
	}

	private void handleVersionWarning(MinecraftClient client) {
		if (client.world == null || client.player == null) {
			return;
		}

		if (!versionCheckFinished || !outdatedVersionDetected || versionWarningSent) {
			return;
		}

		PlayerUtils.sendMessageWithPing("§cYou are using an outdated mod version. §7Current: §f"
				+ localModVersion + " §7Latest: §f" + latestRemoteVersion);
		versionWarningSent = true;
	}

	private void startVersionCheck() {
		localModVersion = resolveLocalModVersion();

		CompletableFuture.supplyAsync(this::fetchRemoteVersion)
				.thenAccept(remoteVersion -> {
					versionCheckFinished = true;
					if (remoteVersion.isEmpty()) {
						return;
					}

					latestRemoteVersion = remoteVersion;
					outdatedVersionDetected = !latestRemoteVersion.equals(localModVersion);
					if (outdatedVersionDetected) {
						LOG.info("Update available: local={} latest={}", localModVersion, latestRemoteVersion);
					}
				})
				.exceptionally(error -> {
					versionCheckFinished = true;
					LOG.warn("Version check failed", error);
					return null;
				});
	}

	private String resolveLocalModVersion() {
		return FabricLoader.getInstance().getModContainer(MODID)
				.map(container -> container.getMetadata().getVersion().getFriendlyString())
				.orElse(VERSION);
	}

	private String fetchRemoteVersion() {
		try {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(VERSION_CHECK_URL))
					.timeout(Duration.ofSeconds(5))
					.GET()
					.build();

			HttpResponse<String> response = VERSION_HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200 || response.body() == null) {
				LOG.warn("Version check request failed with status {}", response.statusCode());
				return "";
			}

			return response.body().replace("\uFEFF", "").trim();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOG.warn("Version check interrupted", e);
			return "";
		} catch (Exception e) {
			LOG.warn("Unable to fetch remote version", e);
			return "";
		}
	}

	private int getGameState() {
		// Game state detection from scoreboard
		// 0 = lobby, 1 = waiting, 2 = in game
		for (String line : PlayerUtils.STRING_SCOREBOARD) {
			String clean = line.replaceAll("§.", "").trim();
			if (clean.contains("Theme:") || clean.contains("Difficulty:")) {
				return 2; // In game
			}
		}
		return 0; // Default to lobby
	}

	private void detectClosestPlatform() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) return;

		BlockPos playerPos = client.player.getBlockPos();
		double closestDist = Double.MAX_VALUE;
		BlockPos nearest = null;

		for (BlockPos platform : platformPositions) {
			double dist = playerPos.getSquaredDistance(platform);
			if (dist < closestDist) {
				closestDist = dist;
				nearest = platform;
			}
		}

		if (nearest != null) {
			closestPlatform = nearest;
			PlayerUtils.debug("§bClosest platform: §f" + String.format("X=%d, Y=%d, Z=%d",
					nearest.getX(), nearest.getY(), nearest.getZ()));
		}
	}

	private String getVariant() {
		if (closestPlatform == null || currentTheme == null || currentTheme.isEmpty()) {
			return "error";
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) return "error";

		String cleanTheme = cleanText(currentTheme).toLowerCase();

		switch (cleanTheme) {
			case "painting":
				return getPaintingVariant();
			case "clownfish":
				return getClownfishVariant();
			default:
				return "";
		}
	}

	private String getPaintingVariant() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) return "";

		BlockPos checkPos = closestPlatform.up(2);
		Block block = client.world.getBlockState(checkPos).getBlock();

		PlayerUtils.debug("Checking painting variant at " + checkPos);

		if (block == Blocks.LIME_WOOL) {
			PlayerUtils.debug("Found lime wool - Horizontal variant");
			return "Horizontal";
		} else if (block == Blocks.YELLOW_WOOL) {
			PlayerUtils.debug("Found yellow wool - Vertical variant");
			return "Vertical";
		}

		PlayerUtils.debug("No matching painting variant found");
		return "";
	}

	private String getClownfishVariant() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) return "";

		BlockPos checkPos = closestPlatform.up(2);
		Block block = client.world.getBlockState(checkPos).getBlock();

		PlayerUtils.debug("Checking clownfish variant at " + checkPos);

		if (block == Blocks.ORANGE_STAINED_GLASS || block == Blocks.ORANGE_STAINED_GLASS_PANE) {
			PlayerUtils.debug("Found orange glass - Medium variant");
			return "Medium";
		} else if (block == Blocks.ORANGE_TERRACOTTA) {
			PlayerUtils.debug("Found orange terracotta - Small variant");
			return "Small";
		}

		PlayerUtils.debug("No matching clownfish variant found");
		return "";
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

	private void onChat(Text message, boolean overlay) {
		if (!Activated) {
			return;
		}
		String messageStr = message.getString();

		Pattern pattern = Pattern.compile("(.*) got a perfect build in (.*)s!");
		Matcher matcher = pattern.matcher(messageStr);
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
					double oldBest = record.bestTime;
					record.bestTime = time;
					double increase = PlayerUtils.round(record.bestTime - oldBest, 1);
					saveTimes();

					String variantDisplay = variant.isEmpty() ? "" : " (" + variant + ")";
					PlayerUtils.sendMessage("§a§lNew Best Time! " + cleanTheme + variantDisplay +
							" (" + cleanDifficulty + "): §b" + time + "§7s §b" + increase + "§7s");
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

	public void showSessionOverview() {
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
			Type type = new TypeToken<HashMap<String, List<BuildRecord>>>() {
			}.getType();
			Map<String, List<BuildRecord>> data = GSON.fromJson(reader, type);
			if (data != null && data.containsKey("themes")) {
				times = data.get("themes");

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