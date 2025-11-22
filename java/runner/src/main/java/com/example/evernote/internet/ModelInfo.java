package com.example.evernote.internet;

public enum ModelInfo {

    SCENARIO_ALL   (0, "v7_c40",       "100", java.time.Duration.ofDays(30).toMillis(), 1, 4),
    SCENARIO_PART  (1, "v8_c40_2020",  "50", java.time.Duration.ofDays(1).toMillis(), 1, 6),
    SCENARIO_LATEST(2, "v8_c40_2024",  "10", 0, 0, 23);

    private final int scenario;
    private final String suffix;
    private final String lastPercentage;
    private final long maxAgeMs;
    private final int hTrainingStartAllowedMin;
    private final int hTrainingStartAllowedMax;

    ModelInfo(int scenario, String suffix, String lastPercentage, long maxAgeMs,
              int hTrainingStartAllowedMin, int hTrainingStartAllowedMax) {
        this.scenario = scenario;
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

    public String getLastPercentage() {
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

    // Optional: damit du von int -> Enum kommst
    public static ModelInfo fromScenario(int scenario) {
        for (ModelInfo mi : values()) {
            if (mi.scenario == scenario) {
                return mi;
            }
        }
        throw new IllegalArgumentException("Unknown ModelInfo scenario: " + scenario);
    }
}

/*
public class ModelInfo {

    public static final int SCENARIO_ALL = 0;
    public static final int SCENARIO_PART = 1;
    public static final int SCENARIO_LATEST = 2;

    public static final String[] V_SUFFIX = new String[]{"v7_c40", "v8_c40_2020", "v8_c40_2024"};
    public static final String[] V_LAST_PERCENTAGE = new String[]{"100", "50", "10"};

    public ModelInfo() {
    }

}
*/