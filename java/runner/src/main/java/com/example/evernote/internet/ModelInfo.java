package com.example.evernote.internet;

import com.example.evernote.LocalStore;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public enum
ModelInfo {

    SCENARIO_ALL   (0),
    SCENARIO_PART  (1),
    SCENARIO_LATEST(2);

    static {
        File file = new File(LocalStore.getSingleton().getMappings().toFile(), "ModelInfo.properties");
        try (var in = new FileInputStream(file)) {
            Properties props = new Properties();
            props.load(in);

            // Für jede Konstante die Werte aus den Properties holen
            for (ModelInfo t : values()) {
                String prefix = t.getScenario()+ ".";

                t.init(
                        props.getProperty(prefix + "suffix"),
                        Integer.parseInt(props.getProperty(prefix + "lastPercentage")),
                        Long.parseLong(props.getProperty(prefix + "maxAgeMs")),
                        Integer.parseInt(props.getProperty(prefix + "hTrainingStartAllowedMin")),
                        Integer.parseInt(props.getProperty(prefix + "hTrainingStartAllowedMax"))
                );
            }

        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final int scenario;
    private String suffix;
    private int lastPercentage;
    private long maxAgeMs;
    private int hTrainingStartAllowedMin;
    private int hTrainingStartAllowedMax;

    ModelInfo(int scenario) {
        this.scenario = scenario;
    }

    private void init(String suffix, int lastPercentage, long maxAgeMs,
              int hTrainingStartAllowedMin, int hTrainingStartAllowedMax) {
        this.suffix = suffix;
        this.lastPercentage = lastPercentage;
        this.maxAgeMs = maxAgeMs;
        this.hTrainingStartAllowedMin = hTrainingStartAllowedMin;
        this.hTrainingStartAllowedMax = hTrainingStartAllowedMax;
    }

    public int getScenario() {
        return scenario;
    }

    public String getSuffix() {
        return suffix;
    }

    public int getLastPercentage() {
        return lastPercentage;
    }

    public long getMaxAgeMs() {
        return maxAgeMs;
    }

    public int gethTrainingStartAllowedMin() {
        return hTrainingStartAllowedMin;
    }

    public int gethTrainingStartAllowedMax() {
        return hTrainingStartAllowedMax;
    }

    public static ModelInfo fromScenario(int scenario) {
        for (ModelInfo mi : values()) {
            if (mi.scenario == scenario) {
                return mi;
            }
        }
        throw new IllegalArgumentException("Unknown ModelInfo scenario: " + scenario);
    }
}