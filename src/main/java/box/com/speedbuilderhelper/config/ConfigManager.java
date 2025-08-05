package box.com.speedbuilderhelper.config;

import box.com.speedbuilderhelper.SpeedBuilderHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.Logger;

import java.io.*;

public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(SpeedBuilderHelper.DIRECTORY, "speedbuildershelper.json");
    private final Logger logger;

    private boolean activated;
    private boolean startingMessage;
    private static boolean debug;
    private String playerName;

    public ConfigManager(Logger logger) {
        this.logger = logger;
        loadConfig();
    }

    public void loadConfig() {
        if (!CONFIG_FILE.exists()) {
            activated = true;
            startingMessage = true;
            debug = false;
            playerName = "";
            saveConfig();
            logger.info("Created Config File " + CONFIG_FILE.getAbsolutePath());
            return;
        }

        try (Reader reader = new FileReader(CONFIG_FILE)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);

            if (json != null) {
                activated = json.has("activated") && json.get("activated").getAsBoolean();
                startingMessage = json.has("startingMessage") && json.get("startingMessage").getAsBoolean();
                debug = json.has("debug") && json.get("debug").getAsBoolean();
                playerName = json.has("playerName") ? json.get("playerName").getAsString() : "";

                logger.info("Loaded config: activated=" + activated +
                        ", startingMessage=" + startingMessage +
                        ", debug=" + debug +
                        ", playerName=" + playerName);
            }
        } catch (IOException e) {
            logger.error("Failed to read config file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveConfig() {
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            JsonObject json = new JsonObject();
            json.addProperty("activated", activated);
            json.addProperty("startingMessage", startingMessage);
            json.addProperty("debug", debug);
            json.addProperty("playerName", playerName != null ? playerName : "");

            GSON.toJson(json, writer);
            logger.info("Saved config: activated=" + activated +
                    ", startingMessage=" + startingMessage +
                    ", debug=" + debug +
                    ", playerName=" + playerName);
        } catch (IOException e) {
            logger.error("Failed to save config file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public boolean isStartingMessage() {
        return startingMessage;
    }

    public void setStartingMessage(boolean startingMessage) {
        this.startingMessage = startingMessage;
    }

    public static boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
}