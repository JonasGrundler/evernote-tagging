package com.example.evernote.internet;

import com.example.evernote.LocalStore;
import com.example.evernote.ServicesClient;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class TrainingClient {

    private static final String CHANGES_FILENAME_BASE = "changes";
    private static final String LAST_TRAINING_FILENAME_BASE = "lastTraining";

    private final BufferedWriter[] bwTracking = new BufferedWriter[ModelInfo.values().length];

    public TrainingClient() {
    }

    public static void main(String[] args) {
        try {
            TrainingClient tc = new TrainingClient();
            tc.train(Integer.parseInt(args[0]));
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    public int train(int scenario) throws Exception {
        int returnValue = 0;
        Path pTracking = getPath(scenario, LAST_TRAINING_FILENAME_BASE);
        if (! pTracking.toFile().exists()) {
            pTracking.toFile().createNewFile();
        }

        long t = System.currentTimeMillis();
        long diff = t - pTracking.toFile().lastModified();

        // update time exceeded?
        ModelInfo mi = ModelInfo.fromScenario(scenario);
        if (diff > mi.getMaxAgeMs()) {
            Path pChanges = getPath(scenario, CHANGES_FILENAME_BASE);

            // are there changes?
            if (pChanges.toFile().exists()) {

                // current time allows for training?
                ZoneId zone = ZoneId.systemDefault();
                LocalDateTime ldt = Instant.ofEpochMilli(t).atZone(zone).toLocalDateTime();
                int hour = ldt.getHour();

                if (hour >= mi.gethTrainingStartAllowedMin() && hour <= mi.gethTrainingStartAllowedMax()) {
                    try {
                        doTraining(scenario);
                        try {
                            pChanges.toFile().delete();
                            pTracking.toFile().setLastModified(t);
                        } catch (Exception e2) {
                            e2.printStackTrace(System.out);
                        }
                    } catch (Exception e) {
                        e.printStackTrace(System.out);
                    }
                } else {
                    System.out.println("scenario " + mi.getScenario() + " : training necessary, but we'll wait until we're in allowed hours from " + mi.gethTrainingStartAllowedMin() + " to " + mi.gethTrainingStartAllowedMax() + ". (currently " + hour + ")");
                }
            } else {
                System.out.println("scenario " + mi.getScenario() + " : nothing to train : no changes occurred");
            }
        } else {
            System.out.println("scenario " + mi.getScenario() + " : last training " + diff + " ms ago, which is less than " + mi.getMaxAgeMs());
        }

        return returnValue;
    }

    private Path getPath(int scenario, String id) {
        return Paths.get(LocalStore.getSingleton().getInternet_single_training().toString(), id + "_" + scenario + ".txt");
    }

    public void trackChange(String line) {
        for (ModelInfo mi : ModelInfo.values()) {
            try {
                if (bwTracking[mi.getScenario()] == null) {
                    bwTracking[mi.getScenario()] = new BufferedWriter(new FileWriter(getPath(mi.getScenario(), CHANGES_FILENAME_BASE).toFile(), true));
                }
                bwTracking[mi.getScenario()].write(line);
                bwTracking[mi.getScenario()].newLine();
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }
    }

    public void endChangeTracking() {
        for (ModelInfo mi : ModelInfo.values()) {
            try {
                if (bwTracking[mi.getScenario()] != null) {
                    bwTracking[mi.getScenario()].close();
                    bwTracking[mi.getScenario()] = null;
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }
    }

    private void doTraining(int scenario) throws Exception {
        ServicesClient.getSingleton().train(scenario);
    }
}