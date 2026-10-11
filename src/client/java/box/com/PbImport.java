package box.com;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Downloads a user's personal bests from speedbuilders.net so they can be imported into the local times file.
 * Has no Minecraft dependencies, so the same file is used on every branch.
 */
public class PbImport {

    private static final Logger LOG = LogManager.getLogger("SPEEDBUILDERS");
    private static final String USER_URL = "https://speedbuilders.net/api/user/";
    private static final Pattern USER_ID = Pattern.compile("\\d{5,25}");
    private static final Pattern SECONDS = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*seconds?", Pattern.CASE_INSENSITIVE);

    public static class Entry {
        public final String theme;
        public final String difficulty;
        public final String variant;
        public final double time;

        Entry(String theme, String difficulty, String variant, double time) {
            this.theme = theme;
            this.difficulty = difficulty;
            this.variant = variant;
            this.time = time;
        }
    }

    public static class Result {
        public String error;
        public List<Entry> entries = new ArrayList<>();
        public int skipped;
    }

    public static boolean isValidUserId(String userId) {
        return userId != null && USER_ID.matcher(userId).matches();
    }

    /** Fetches on a background thread, then calls back on that same thread. */
    public static void fetchAsync(String userId, Consumer<Result> callback) {
        Thread thread = new Thread(() -> {
            Result result;
            try {
                result = fetch(userId);
            } catch (Exception e) {
                LOG.warn("PB import failed: " + e);
                result = new Result();
                result.error = "Could not reach speedbuilders.net (" + e.getClass().getSimpleName() + ")";
            }
            callback.accept(result);
        }, "SpeedBuildersHelper-PbImport");
        thread.setDaemon(true);
        thread.start();
    }

    private static Result fetch(String userId) throws Exception {
        Result result = new Result();
        HttpURLConnection connection = (HttpURLConnection) new URL(USER_URL + userId).openConnection();
        try {
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "SpeedBuildersHelper");
            connection.setRequestProperty("Accept", "application/json");

            if (connection.getResponseCode() != 200) {
                result.error = "speedbuilders.net returned HTTP " + connection.getResponseCode();
                return result;
            }

            try (Reader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
                JsonArray entries = root.getAsJsonArray("leaderboardEntries");

                for (JsonElement element : entries) {
                    Entry entry = parseEntry(element.getAsJsonObject());
                    if (entry == null) {
                        result.skipped++;
                    } else {
                        result.entries.add(entry);
                    }
                }
            }
        } finally {
            connection.disconnect();
        }

        if (result.entries.isEmpty()) {
            result.error = "No builds found for user ID " + userId + ". Double check the ID";
        }
        return result;
    }

    private static Entry parseEntry(JsonObject json) {
        if (!json.has("category") || !json.has("build") || !json.has("time")) {
            return null;
        }

        Matcher matcher = SECONDS.matcher(json.get("time").getAsString());
        if (!matcher.find()) {
            return null;
        }

        String difficulty = json.get("category").getAsString().trim();
        String build = json.get("build").getAsString().trim();
        double time = Double.parseDouble(matcher.group(1));

        // The leaderboard lists variants as part of the build name, the mod stores them separately
        String lower = build.toLowerCase();
        if (lower.startsWith("painting ")) {
            return new Entry("Painting", difficulty, build.substring("painting ".length()).trim(), time);
        }
        if (lower.startsWith("clownfish ")) {
            String size = build.substring("clownfish ".length()).trim();
            return new Entry("ClownFish", difficulty, size.equalsIgnoreCase("Big") ? "Medium" : size, time);
        }
        return new Entry(build, difficulty, "", time);
    }
}
