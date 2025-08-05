package box.com.speedbuilderhelper.managers;

import box.com.speedbuilderhelper.SpeedBuilderHelper;
import box.com.speedbuilderhelper.model.BuildRecord;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimeManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File TIMES_FILE = new File(SpeedBuilderHelper.Directory, "speedbuilder_times.json");

    private final Logger logger;
    private List<BuildRecord> times = new ArrayList<>();

    public TimeManager(Logger logger) {
        this.logger = logger;
        loadTimes();
    }

    public void loadTimes() {
        if (!TIMES_FILE.exists()) {
            try {
                TIMES_FILE.createNewFile();
                saveTimes();
                logger.info("Created Times File " + TIMES_FILE.getAbsolutePath());
            } catch (IOException e) {
                logger.error("Failed to create times file: " + e.getMessage());
                e.printStackTrace();
            }
            return;
        }

        try (Reader reader = new FileReader(TIMES_FILE)) {
            Type type = new TypeToken<HashMap<String, List<BuildRecord>>>() {}.getType();
            Map<String, List<BuildRecord>> data = GSON.fromJson(reader, type);
            if (data != null && data.containsKey("themes")) {
                times = data.get("themes");
                migrateOldRecords();
            }
        } catch (IOException e) {
            logger.error("Failed to read times file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void migrateOldRecords() {
        boolean needsMigration = false;
        for (BuildRecord record : times) {
            if (record.getVariant() == null) {
                needsMigration = true;
                break;
            }
        }

        if (needsMigration) {
            logger.info("Migrating old records to new format with variant field");
            List<BuildRecord> newRecords = new ArrayList<>();
            for (BuildRecord oldRecord : times) {
                if (oldRecord.getVariant() == null) {
                    newRecords.add(new BuildRecord(
                            oldRecord.getTheme(),
                            oldRecord.getDifficulty(),
                            "",
                            oldRecord.getBestTime(),
                            oldRecord.isNewBest()
                    ));
                } else {
                    newRecords.add(oldRecord);
                }
            }
            times = newRecords;
            saveTimes();
            logger.info("Migration completed successfully");
        }
    }

    public void saveTimes() {
        try (Writer writer = new FileWriter(TIMES_FILE)) {
            Map<String, List<BuildRecord>> data = new HashMap<>();
            data.put("themes", times);
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<BuildRecord> getTimes() {
        return times;
    }

    public double getBestTime(String theme, String difficulty, String variant) {
        for (BuildRecord record : times) {
            if (record.getTheme().equalsIgnoreCase(theme) &&
                    record.getDifficulty().equalsIgnoreCase(difficulty) &&
                    record.getVariant().equals(variant)) {
                return record.getBestTime();
            }
        }
        return Double.MAX_VALUE;
    }

    public boolean updateTime(String theme, String difficulty, String variant, double time) {
        String cleanTheme = theme.replaceAll("§.", "");
        String cleanDifficulty = difficulty.replaceAll("§.", "");
        boolean isNewBest = false;

        for (BuildRecord record : times) {
            if (record.getTheme().equals(cleanTheme) &&
                    record.getDifficulty().equals(cleanDifficulty) &&
                    record.getVariant().equals(variant)) {

                if (time < record.getBestTime()) {
                    record.setBestTime(time);
                    saveTimes();
                    isNewBest = true;
                }
                return isNewBest;
            }
        }

        times.add(new BuildRecord(cleanTheme, cleanDifficulty, variant, time));
        saveTimes();
        return true;
    }
}