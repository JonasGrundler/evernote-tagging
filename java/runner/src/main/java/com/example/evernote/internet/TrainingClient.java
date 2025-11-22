package com.example.evernote.internet;

import com.example.evernote.LocalStore;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class TrainingClient {

    private static final String PV_PYTHON = System.getenv("PYTHON") + "\\python";
    private static final String PV_SCRIPT = System.getenv("PADDLE_DIR") + "\\train_tag_model4.py";
    private static final String P_SUFFIX = "--suffix";
    private static final String P_LAST_PERCENTAGE = "--last_percentage";
    private static final String P_CSV_PATH = "--csv_path";
    private static final String P_INT_PATH = "--int_path";

    private static final String CHANGES_FILENAME_BASE = "changes";
    private static final String LAST_TRAINING_FILENAME_BASE = "lastTraining";
    private BufferedWriter[] bwTracking = new BufferedWriter[ModelInfo.values().length];

    private ProcessBuilder pbTraining;

    public static void main(String[] args) {
        try {
            TrainingClient tc = new TrainingClient();
            if (args.length == 1) {
                tc.train(ModelInfo.fromScenario(Integer.parseInt(args[0])));
            } else {
                List<String> list = new ArrayList<>();
                for (String arg : args) {
                    list.add(arg);
                }
                tc.train(list);
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    public TrainingClient() {
    }

    public int train(ModelInfo mi) throws IOException {
        int returnValue = 0;
        Path pTracking = getPath(mi, LAST_TRAINING_FILENAME_BASE);
        if (! pTracking.toFile().exists()) {
            pTracking.toFile().createNewFile();
        }

        long t = System.currentTimeMillis();
        long diff = t - pTracking.toFile().lastModified();

        // update time exceeded?
        if (diff > mi.getMaxAgeMs()) {
            Path pChanges = getPath(mi, CHANGES_FILENAME_BASE);

            // are there changes?
            if (pChanges.toFile().exists()) {

                // current time allows for training?
                ZoneId zone = ZoneId.systemDefault();
                LocalDateTime ldt = Instant.ofEpochMilli(t).atZone(zone).toLocalDateTime();
                int hour = ldt.getHour();

                if (hour >= mi.gethTrainingStartAllowedMin() && hour <= mi.gethTrainingStartAllowedMax()) {
                    List<String> parameters = new ArrayList<>();
                    parameters.add(P_SUFFIX);
                    parameters.add(mi.getSuffix());
                    parameters.add(P_CSV_PATH);
                    parameters.add(Paths.get(LocalStore.getSingleton().getEnex_batch_csv().toString(), "summary.csv").toString());
                    parameters.add(P_LAST_PERCENTAGE);
                    parameters.add(mi.getLastPercentage());
                    if (mi == ModelInfo.SCENARIO_LATEST) {
                        parameters.add(P_INT_PATH);
                        parameters.add(Paths.get(LocalStore.getSingleton().getInternet_single_csv().toString(), "summary.csv").toString());
                    }
                    returnValue = train(parameters);

                    if (returnValue == 0) {
                        pChanges.toFile().delete();
                        pTracking.toFile().setLastModified(t);
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


    // for testing purposes only, no tracking here
    private int train(List<String> args) throws IOException {
        List<String> parameters = new ArrayList<>();
        parameters.add(PV_PYTHON);
        parameters.add(PV_SCRIPT);
        parameters.addAll(args);

        System.out.println("training with " + parameters);

        pbTraining = new ProcessBuilder(parameters);
        pbTraining.directory(new File(System.getenv("PADDLE_DIR")));
        pbTraining.redirectErrorStream(true);

        int returnValue = train();

        return returnValue;
    }

    private Path getPath(ModelInfo mi, String id) {
        Path path = Paths.get(LocalStore.getSingleton().getInternet_single_training().toString(), id + "_" + mi.getScenario() + ".txt");
        return path;
    }

    public void trackChange(String line) {
        for (ModelInfo mi : ModelInfo.values()) {
            try {
                if (bwTracking[mi.getScenario()] == null) {
                    bwTracking[mi.getScenario()] = new BufferedWriter(new FileWriter(getPath(mi, CHANGES_FILENAME_BASE).toFile(), true));
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

    private int train() throws IOException {
        try {
            System.out.println("starting training ...");

            Process process = pbTraining.start();

            BufferedReader infPyOut = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

            String line;
            while ((line = infPyOut.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            System.out.println("Exit-Code: " + exitCode);
            return exitCode;
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return -1;
    }
}
