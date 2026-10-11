package box.com;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keeps the speedbuilders.net world records in a local cache so the leaderboard endpoints are only
 * hit once every {@link #CACHE_TTL_MS} (and no more than once every {@link #RETRY_DELAY_MS} after a failure).
 * Has no Minecraft dependencies, so the same file is used on every branch.
 */
public class WorldRecords {

    private static final Logger LOG = LogManager.getLogger("SPEEDBUILDERS");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String API_BASE = "https://speedbuilders.net/api/leaderboard/";
    private static final String[] DIFFICULTIES = {"simple", "easy", "medium", "hard", "insane"};

    private static final long CACHE_TTL_MS = 6L * 60 * 60 * 1000;
    private static final long RETRY_DELAY_MS = 15L * 60 * 1000;
    private static final Pattern NUMBER = Pattern.compile("(\\d+(?:\\.\\d+)?)");

    public static class Record {
        public String build;
        public double time;
        public String user;
        public String clickingMethod;

        Record(String build, double time, String user, String clickingMethod) {
            this.build = build;
            this.time = time;
            this.user = user;
            this.clickingMethod = clickingMethod;
        }
    }

    private static class Cache {
        long fetchedAt;
        Map<String, List<Record>> records = new HashMap<>();
    }

    private static File cacheFile;
    // difficulty -> normalised build name -> record. Replaced wholesale, never mutated in place.
    private static volatile Map<String, Map<String, Record>> records = new HashMap<>();
    private static volatile long fetchedAt = 0;
    private static volatile long lastAttempt = 0;
    private static final AtomicBoolean refreshing = new AtomicBoolean(false);

    public static void init(File directory) {
        cacheFile = new File(directory, "worldrecords_cache.json");
        loadCache();
        refreshIfStale();
    }

    /** Looks up the world record for a build. Returns null if unknown (or the cache hasn't loaded yet). */
    public static Record get(String difficulty, String theme, String variant) {
        refreshIfStale();

        Map<String, Record> byBuild = records.get(normalise(difficulty));
        if (byBuild == null) {
            return null;
        }

        String base = theme == null ? "" : theme;
        String extra = variant == null ? "" : variant;
        Record found = byBuild.get(normalise(base + extra));
        if (found == null && extra.equalsIgnoreCase("Medium")) {
            // The leaderboard calls the larger clownfish "Big"
            found = byBuild.get(normalise(base + "Big"));
        }
        return found;
    }

    /** Called after the player beats the record, so we don't announce the same record again before the next refresh. */
    public static void set(String difficulty, String theme, String variant, double time, String user) {
        Map<String, Map<String, Record>> copy = new HashMap<>();
        for (Map.Entry<String, Map<String, Record>> entry : records.entrySet()) {
            copy.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }

        Map<String, Record> byBuild = copy.get(normalise(difficulty));
        if (byBuild == null) {
            byBuild = new HashMap<>();
            copy.put(normalise(difficulty), byBuild);
        }

        String extra = variant == null ? "" : variant;
        String key = normalise(theme + extra);
        Record old = byBuild.get(key);
        if (old == null && extra.equalsIgnoreCase("Medium")) {
            key = normalise(theme + "Big");
            old = byBuild.get(key);
        }
        byBuild.put(key, new Record(old != null ? old.build : (theme + " " + extra).trim(), time, user,
                old != null ? old.clickingMethod : ""));
        records = copy;
    }

    public static String formatTime(double seconds) {
        double rounded = Math.round(seconds * 100.0) / 100.0;
        if (rounded == Math.floor(rounded)) {
            return String.valueOf((long) rounded);
        }
        return String.valueOf(rounded);
    }

    private static String normalise(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static void refreshIfStale() {
        long now = System.currentTimeMillis();
        if (now - fetchedAt < CACHE_TTL_MS || now - lastAttempt < RETRY_DELAY_MS) {
            return;
        }
        if (!refreshing.compareAndSet(false, true)) {
            return;
        }

        lastAttempt = now;
        Thread thread = new Thread(() -> {
            try {
                refresh();
            } catch (Exception e) {
                LOG.warn("World record refresh failed", e);
            } finally {
                refreshing.set(false);
            }
        }, "SpeedBuildersHelper-WorldRecords");
        thread.setDaemon(true);
        thread.start();
    }

    private static void refresh() {
        Map<String, Map<String, Record>> updated = new HashMap<>(records);
        int succeeded = 0;

        for (String difficulty : DIFFICULTIES) {
            Map<String, Record> fetched = fetchDifficulty(difficulty);
            if (fetched != null) {
                updated.put(difficulty, fetched);
                succeeded++;
            }
        }

        if (succeeded == 0) {
            LOG.warn("Could not update world records, will retry later");
            return;
        }

        records = updated;
        if (succeeded == DIFFICULTIES.length) {
            fetchedAt = System.currentTimeMillis();
        }
        saveCache();
        LOG.info("Updated world records (" + succeeded + "/" + DIFFICULTIES.length + " leaderboards)");
    }

    private static Map<String, Record> fetchDifficulty(String difficulty) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(API_BASE + difficulty + "-builds").openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "SpeedBuildersHelper");
            connection.setRequestProperty("Accept", "application/json");

            if (connection.getResponseCode() != 200) {
                LOG.warn("World record request for " + difficulty + " returned " + connection.getResponseCode());
                return null;
            }

            try (Reader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
                JsonArray entries = root.get(difficulty + "Builds").getAsJsonArray();

                Map<String, Record> result = new HashMap<>();
                for (JsonElement element : entries) {
                    JsonObject entry = element.getAsJsonObject();
                    Record record = parseEntry(entry);
                    if (record == null) {
                        continue;
                    }

                    // The API lists some builds more than once, keep the fastest
                    String key = normalise(record.build);
                    Record existing = result.get(key);
                    if (existing == null || record.time < existing.time) {
                        result.put(key, record);
                    }
                }
                return result;
            }
        } catch (Exception e) {
            LOG.warn("Unable to fetch " + difficulty + " world records: " + e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static Record parseEntry(JsonObject entry) {
        if (!entry.has("build") || !entry.has("time") || !entry.has("user")) {
            return null;
        }

        Matcher matcher = NUMBER.matcher(entry.get("time").getAsString());
        if (!matcher.find()) {
            return null;
        }

        String method = entry.has("clickingMethod") && !entry.get("clickingMethod").isJsonNull()
                ? entry.get("clickingMethod").getAsString() : "";
        return new Record(entry.get("build").getAsString(), Double.parseDouble(matcher.group(1)),
                entry.get("user").getAsString(), method);
    }

    private static void loadCache() {
        if (cacheFile == null || !cacheFile.exists()) {
            return;
        }

        try (Reader reader = new FileReader(cacheFile)) {
            Type type = new TypeToken<Cache>() {}.getType();
            Cache cache = GSON.fromJson(reader, type);
            if (cache == null || cache.records == null) {
                return;
            }

            Map<String, Map<String, Record>> loaded = new HashMap<>();
            for (Map.Entry<String, List<Record>> entry : cache.records.entrySet()) {
                Map<String, Record> byBuild = new HashMap<>();
                for (Record record : entry.getValue()) {
                    byBuild.put(normalise(record.build), record);
                }
                loaded.put(entry.getKey(), byBuild);
            }
            records = loaded;
            fetchedAt = cache.fetchedAt;
        } catch (Exception e) {
            LOG.warn("Failed to read world record cache: " + e);
        }
    }

    private static void saveCache() {
        if (cacheFile == null) {
            return;
        }

        Cache cache = new Cache();
        cache.fetchedAt = fetchedAt;
        for (Map.Entry<String, Map<String, Record>> entry : records.entrySet()) {
            cache.records.put(entry.getKey(), new ArrayList<>(entry.getValue().values()));
        }

        try (Writer writer = new FileWriter(cacheFile)) {
            GSON.toJson(cache, writer);
        } catch (IOException e) {
            LOG.warn("Failed to save world record cache: " + e);
        }
    }
}
