package com.example.evernote.internet;

import com.evernote.edam.error.EDAMErrorCode;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteMetadata;
import com.evernote.edam.notestore.NotesMetadataList;
import com.evernote.edam.notestore.NotesMetadataResultSpec;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.NoteSortOrder;
import com.evernote.edam.type.Resource;
import com.example.evernote.BuildCSV;
import com.example.evernote.LocalStore;
import com.example.evernote.ServicesClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class EvernoteTagging {

    private InternetHelper internetHelper;

    private TrainingClient tc;

    private boolean reTag = false;

    private static final int INTERVAL = 60 * 60 * 1000; // 12x/Tag
    private static final int COUNT = 30; // 180/Tag bei 12 Durchläufen, pro Note werden 2 API Calls gemacht

    private EvernoteTagging(boolean reTag) throws Exception {
        this.reTag = reTag;
        internetHelper = new InternetHelper();

        tc = new TrainingClient();

        System.out.println("targetNotebookName:" + RemoteNote.EN_USER_CONF_NB);
        System.out.println("Notebook Mappings:");
        Set<Map.Entry<String, String>> set = FromToNotebooks.getSingleton().getNamePairs().entrySet();
        if (!set.isEmpty()) {
            for (Map.Entry<String, String> e : FromToNotebooks.getSingleton().getNamePairs().entrySet()) {
                System.out.println("from:" + e.getKey() + " to:" + e.getValue());
            }
        } else {
            System.out.println("No mappings found.");
        }
    }

    public static void main(String[] args) {
        try {
            boolean reTag = args != null && args.length == 1 && args[0].equals("retag=true");
            EvernoteTagging et = new EvernoteTagging(reTag);

            // 1x pro Stunde
            while (true) {
                int taggingCount = 0;

                // tagging
                long t = System.currentTimeMillis();
                try {
                    System.out.println("start processing");
                    taggingCount = et.tagNotes();
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }
                System.out.println("done processing");
                System.out.println("count=" + taggingCount);
                System.out.println("finished processing in " + (System.currentTimeMillis() - t) / 1000 + " seconds");
                long sleep = 60 * 60 * 1000 - (System.currentTimeMillis() - t);
                System.out.println("should sleep for " + sleep / 1000 + " seconds");

                // checking if files have been removed from tagging-nb and remove and add to summary.csv
                int moveCount = 0;
                if (sleep > 10 * 60 * 1000 && taggingCount < COUNT / 2) {
                    System.out.println("enough time for retraining and count not excessively used");
                    System.out.println("start moving");
                    try {
                        moveCount = et.addNotesToCSVAndTrackChanges();
                    } catch (Exception e) {
                        e.printStackTrace(System.out);
                    }
                    System.out.println("done moving, moveCount=" + moveCount);
                    sleep = 60 * 60 * 1000 - (System.currentTimeMillis() - t);
                }

                // training
                //if (moveCount > 0) {
                System.out.println("start training");
                try {
                    et.train();
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }
                System.out.println("done training");
                sleep = 60 * 60 * 1000 - (System.currentTimeMillis() - t);
                //}

                if (sleep > 0) {

                    Instant now = Instant.now();
                    Instant later = now.plusMillis(sleep);
                    ZonedDateTime laterLocal = later.atZone(ZoneId.systemDefault());
                    String formatted = laterLocal.format(DateTimeFormatter.ofPattern("HH:mm:ss"));

                    System.out.println("sleeping now for " + sleep / 1000 + " seconds, continue at " + formatted);
                    Thread.sleep(sleep);
                }

            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    public void train() throws Exception {
        for (ModelInfo mi : ModelInfo.values()) {
            tc.train(mi.getScenario());
        }
    }

    private String enmlFromPlain(String text) {
        String esc = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\">" + "<en-note>" + esc + "</en-note>";
    }

    private boolean isNumber(String s) {
        try {
            Integer.parseInt(s.trim());
            return true;
        } catch (Exception e) {}
        return false;
    }

    public int addNotesToCSVAndTrackChanges() {

        System.out.println("ok add Notes");
        int moveCount = 0;
        int count = 0;

        // read all possible files which have been moved out of the tagging folder... max COUNT
        Set<String> notMovedNotes = new HashSet<>();
        for (String targetNotebookName : FromToNotebooks.getSingleton().getNamePairs().values()) {
            try {
                NoteFilter f = new NoteFilter();
                f.setNotebookGuid(RemoteNotebookStore.getSingleton().getGuid(targetNotebookName));
                NotesMetadataResultSpec spec = new NotesMetadataResultSpec();
                NotesMetadataList meta = RemoteNoteStore.getSingleton().getNoteStore().findNotesMetadata(f, 0, 50, spec);

                for (NoteMetadata nm : meta.getNotes()) {
                    notMovedNotes.add(nm.getGuid());
                }
                count++;
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }

        Map<String, String> noteguids = new HashMap<>();
        File[] files = new File(LocalStore.getSingleton().getInternet_single_tagged().toString()).listFiles();

        for (File file : files) {
            if (file.getName().endsWith(".csv")) {
                String guid = file.getName().substring(0, file.getName().length() - 4);
                if (! notMovedNotes.contains(guid)) {
                    try {
                        BufferedReader br = new BufferedReader(new FileReader(file));
                        br.readLine();
                        String line = br.readLine();
                        // line without tags (in case there are...)
                        line = line.substring(0, line.lastIndexOf(",\""));
                        br.close();
                        noteguids.put(guid, line);
                        if (noteguids.size() >= COUNT) {
                            break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace(System.out);
                    }
                }
            }
        }

        // append info
        if (noteguids.size() > 0) {
            Map<String, List<String>> noteguidToTagguids = new HashMap<>();
            try {
                NoteFilter filter = new NoteFilter();
                filter.setOrder(NoteSortOrder.UPDATED.getValue()); // sort by updated
                filter.setAscending(false);                        // neueste zuerst

                NotesMetadataResultSpec spec = new NotesMetadataResultSpec();
                spec.setIncludeTagGuids(true); // falls du Tags als GUIDs brauchst

                NotesMetadataList result =
                        RemoteNoteStore.getSingleton().getNoteStore().findNotesMetadata(filter, 0, 100, spec);

                for (NoteMetadata nm : result.getNotes()) {
                    noteguidToTagguids.put(nm.getGuid(), nm.getTagGuids());
                }
                count++;
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }

            for (Map.Entry<String, String> e : noteguids.entrySet()) {
                try {
                    if (! noteguidToTagguids.containsKey(e.getKey())) {
                        count++;
                        try {
                            Note note = RemoteNoteStore.getSingleton().getNoteStore().getNote(e.getKey(), false, false, false, false);
                            if (note.isSetDeleted() || ! note.isActive()) {
                                continue;
                            }
                            noteguidToTagguids.put(e.getKey(), note.getTagGuids());
                        } catch (Exception e2) {
                            continue;
                        }
                    }
                    moveCount++;

                    // move csv and log changes
                    Path source = Paths.get(LocalStore.getSingleton().getInternet_single_tagged().toString(), e.getKey() + ".csv");
                    Path target = Paths.get(LocalStore.getSingleton().getInternet_single_csv().toString(), e.getKey() + ".csv");
                    BufferedReader br = new BufferedReader(new FileReader(source.toFile()));
                    BufferedWriter bw = new BufferedWriter(new FileWriter(target.toFile()));
                    // header
                    bw.write(br.readLine());
                    bw.newLine();

                    // rest (only 1 line)
                    String line = br.readLine();
                    br.close();

                    StringBuffer sb = new StringBuffer();
                    sb.append(line, 0, line.length() - 1);
                    if (noteguidToTagguids.get(e.getKey()) != null) {
                        for (int i = 0; i < noteguidToTagguids.get(e.getKey()).size(); i++) {
                            sb.append(RemoteTagStore.getSingleton().getNameFromGuid(noteguidToTagguids.get(e.getKey()).get(i)));
                            count++;
                            if (i < noteguidToTagguids.get(e.getKey()).size() - 1) {
                                sb.append(", ");
                            }
                        }
                    }
                    sb.append("\"");
                    bw.write(sb.toString());
                    bw.newLine();
                    bw.close();

                    // log changes
                    tc.trackChange(sb.toString());

                    // delete from tagged
                    for (File file : files) {
                        if (file.getName().indexOf(e.getKey()) >= 0) {
                            if (file.exists()) {
                                boolean deleted = file.delete();
                            } else {
                                System.out.println("file to delete " + file.getAbsolutePath() + " didn't exist anymore");
                            }
                        }
                    }
                } catch (Exception e2) {
                    e2.printStackTrace(System.out);
                }
            }
            tc.endChangeTracking();

            // write summary
            try {
                File summary = new File(LocalStore.getSingleton().getInternet_single_csv().toFile(), "summary.csv");
                BufferedWriter bw = new BufferedWriter(new FileWriter(summary));
                bw.write(BuildCSV.HEADLINE);
                bw.newLine();

                File[] csvs = LocalStore.getSingleton().getInternet_single_csv().toFile().listFiles();
                for (File file : csvs) {
                    if (!file.getCanonicalFile().equals(summary.getCanonicalFile())) {
                        BufferedReader br = new BufferedReader(new FileReader(file));
                        br.readLine();
                        bw.write(br.readLine());
                        bw.newLine();
                        br.close();
                    }
                }
                bw.close();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return moveCount;
    }

    //... read notes from disk, check which are not in 00 Eingang/01 Tagged - retrieve and add to interims csv; let the thing run at night
    //... remove files from notes cache once processed

    public int tagNotes() throws Exception {
        int count = 0;
        for (Map.Entry<String, String> fromToNotebookEntry : FromToNotebooks.getSingleton().getNamePairs().entrySet()) {
            try {
                // reload in case...
                RemoteTagStore.getSingleton().init();
                EMailToTags.getSingleton().init();

                NoteFilter f = new NoteFilter();
                f.setNotebookGuid(RemoteNotebookStore.getSingleton().getGuid(fromToNotebookEntry.getKey()));

                NotesMetadataResultSpec spec = new NotesMetadataResultSpec();
                spec.setIncludeTitle(true);
                spec.setIncludeUpdateSequenceNum(true);
                spec.setIncludeAttributes(true);

                NotesMetadataList meta = RemoteNoteStore.getSingleton().getNoteStore().findNotesMetadata(f, 0, 100, spec);
                // → liefert nur Notizen aus genau diesem Notizbuch (per Name gefiltert)

                List<NoteMetadata> l = meta.getNotes();

                String targetNotebookGuid = RemoteNotebookStore.getSingleton().getGuid(fromToNotebookEntry.getValue());
                for (NoteMetadata nm : l) {
                    try {
                        if (count < COUNT && (reTag || !LocalNoteStore.getSingleton().isTagged(nm.getGuid(), nm.getUpdateSequenceNum()))) {
                            System.out.println("<<<<<<<<<<<<<<<<< count " + count + " >>>>>>>>>>>>>>>>>>>");
                            LocalNoteStore.getSingleton().unTag(nm.getGuid(), nm.getUpdateSequenceNum());
                            //System.out.println("attributes:" + nm.getAttributes().getAuthor() + " " + nm.getAttributes().toString());

                            Note note = LocalNoteStore.getSingleton().load(nm.getGuid(), nm.getUpdateSequenceNum());
                            if (note == null) {
                                note = RemoteNoteStore.getSingleton().getNoteStore().getNote(nm.getGuid(), true, true, true, true);
                                LocalNoteStore.getSingleton().save(note);
                                System.out.println("note from internet:" + note.getTitle());
                                count++;
                            } else {
                                System.out.println("note from store:" + note.getTitle());
                            }

                            System.out.println("aaaaaaaaaaaaaaaaaaaaaaa");
                            System.out.println("GUID:        " + note.getGuid());
                            System.out.println("nb GUID:     " + note.getNotebookGuid());
                            System.out.println("Title:       " + note.getTitle());
                            System.out.println("Content:     " + (note.getContent() == null ? "NULL" : "OK"));
                            System.out.println("NotebookGuid:" + note.getNotebookGuid());
                            System.out.println("TagNames:    " + note.getTagNames());
                            System.out.println("TagGUIDs:    " + note.getTagGuids());
                            if (note.getTagNames() != null) {
                                for (int i = 0; i < note.getTagNames().size(); i++) {
                                    String t = note.getTagNames().get(i);
                                    if (t == null) {
                                        System.out.println("Null-Tag an Position " + i);
                                    }
                                }
                            }
                            if (note.getTagGuids() != null) {
                                for (int i = 0; i < note.getTagGuids().size(); i++) {
                                    String t = note.getTagGuids().get(i);
                                    if (t == null) {
                                        System.out.println("Null-Tag an Position " + i);
                                    }
                                }
                            }
                            System.out.println("aaaaaaaaaaaaaaaaaaaaaa");

                            // title for paper documents
                            setTitleForPaperDocuments(nm, note);

                            // E-Mails
                            setTagsFromEMails(nm, note);

                            // tagging from machine learning
                            String line = setTagsFromPredictions(note);

                            // Keywords
                            setTagsFromKeywords(note, nm, line);

                            // move to tagging folder
                            note.setNotebookGuid(targetNotebookGuid);

                            System.out.println("xxxxxxxxxxxxxxxxxxxxxxx");
                            System.out.println("GUID:        " + note.getGuid());
                            System.out.println("nb GUID:     " + note.getNotebookGuid());
                            System.out.println("Title:       " + note.getTitle());
                            System.out.println("Content:     " + (note.getContent() == null ? "NULL" : "OK"));
                            System.out.println("NotebookGuid:" + note.getNotebookGuid());
                            System.out.println("TagNames:    " + note.getTagNames());
                            System.out.println("TagGUIDs:    " + note.getTagGuids());
                            if (note.getTagNames() != null) {
                                for (int i = 0; i < note.getTagNames().size(); i++) {
                                    String t = note.getTagNames().get(i);
                                    if (t == null) {
                                        System.out.println("Null-Tag an Position " + i);
                                    }
                                }
                            }
                            if (note.getTagGuids() != null) {
                                for (int i = 0; i < note.getTagGuids().size(); i++) {
                                    String t = note.getTagGuids().get(i);
                                    if (t == null) {
                                        System.out.println("Null-Tag an Position " + i);
                                    }
                                }
                            }
                            System.out.println("xxxxxxxxxxxxxxxxxxxxxxx");


                            note = RemoteNoteStore.getSingleton().getNoteStore().updateNote(note);
                            count++;

                            LocalNoteStore.getSingleton().save(note);
                            LocalNoteStore.getSingleton().tag(nm.getGuid(), note.getUpdateSequenceNum());

                            // verschieben in tagged folder
                            for (File file : new File(LocalStore.getSingleton().getInternet_single_notes().toString()).listFiles()) {
                                if (file.getName().indexOf(note.getGuid()) >= 0) {
                                    Path target = Paths.get(LocalStore.getSingleton().getInternet_single_tagged().toString(), file.getName());
                                    Files.move(Paths.get(file.toURI()), target, StandardCopyOption.REPLACE_EXISTING);
                                }
                            }
                        } else {
                            if (count >= COUNT) {
                                System.out.println("count maximum achieved");
                                break;
                            }
                            if (LocalNoteStore.getSingleton().isTagged(nm.getGuid(), nm.getUpdateSequenceNum())) {
                                System.out.println("already processed:" + nm.getTitle());
                            }
                        }
                    } catch (EDAMSystemException e) {
                        if (e.getMessage() != null && e.getMessage().startsWith("Attempt updateNote where RTE room has already been open")) {
                            // skip note and try again
                        } else {
                            e.printStackTrace(System.out);
                            throw e;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
                if (e instanceof EDAMUserException) {
                    EDAMUserException eue = (EDAMUserException) e;
                    if (eue.getErrorCode() == EDAMErrorCode.AUTH_EXPIRED) {
                        LocalTokenStore.getSingleton().clear();
                        System.out.println("Auth expired. Token wurde gelöscht. Bitte neu autorisieren: /evernote/oauth/start");
                    }
                }
                throw e;
            }
        }
        return count;
    }

    private String setTagsFromPredictions(Note note) {
        String line = "";
        List<String> predictedTagsAll = null;
        List<String> predictedTags4 = null;
        List<String> predictedTags1 = null;
        try {
            internetHelper.parseAndWrite(note);

            ZoneId zone = ZoneId.of("Europe/Berlin");
            Instant instant = Instant.ofEpochMilli(note.getCreated());
            ZonedDateTime zdt = instant.atZone(zone);

            File csv = new File (internetHelper.getTargetDir().toFile(), note.getGuid() + ".csv");
            line = BuildCSV.writeSingleLine(csv, internetHelper.getTargetDir().toFile(), note.getGuid(), String.valueOf(zdt.getYear()));
            System.out.println("csvLine:" + csv.getAbsolutePath());
            predictedTagsAll = ServicesClient.getSingleton().infer(ModelInfo.SCENARIO_ALL.getScenario(), csv.getAbsolutePath());
            predictedTags4 = ServicesClient.getSingleton().infer(ModelInfo.SCENARIO_PART.getScenario(), csv.getAbsolutePath());
            predictedTags1 = ServicesClient.getSingleton().infer(ModelInfo.SCENARIO_LATEST.getScenario(), csv.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        if (predictedTagsAll != null) {

            Set<String> allPredictedTags = new HashSet<>();
            allPredictedTags.addAll(predictedTagsAll);
            allPredictedTags.addAll(predictedTags4);
            allPredictedTags.addAll(predictedTags1);

            Set<String> selectedPredictedTagsGuids = new HashSet<>();
            for (String t : allPredictedTags) {
                t = t.trim();
                if (t.length() == 0) continue;
                if (isNumber(t)) continue;
                if (t.equals("done")) continue;
                String tagguid = RemoteTagStore.getSingleton().getGuidFromName(t);
                if (tagguid != null) {
                    selectedPredictedTagsGuids.add(tagguid);
                } else {
                    System.out.println("Warning: tag guid for predicted tag '" + t + "' not found in:" + RemoteTagStore.getSingleton().getTags());
                }
                if (t.equals("Steuer")) {
                    String year = String.valueOf(Instant.ofEpochMilli(note.getCreated()).atZone(ZoneId.systemDefault()).getYear());
                    System.out.println("adding year:" + year + " (" + RemoteTagStore.getSingleton().getGuidFromName(year) + ")");
                    selectedPredictedTagsGuids.add(RemoteTagStore.getSingleton().getGuidFromName(year));
                }
            }

            for (String guid : selectedPredictedTagsGuids) {
                if (guid != null) {
                    if (note.getTagGuids() == null || !note.getTagGuids().contains(guid)) {
                        note.addToTagGuids(guid);
                    }
                } else {
                    System.out.println("Warning: guid was null");
                }
            }
        }
        return line;
    }

    private void setTagsFromTagnames (NoteMetadata nm, Note note, List<String> tagnames) {
        Set<String> missingGuids = new HashSet<>();
        for (String tag : tagnames) {
            String guid = RemoteTagStore.getSingleton().getGuidFromName(tag);
            if (nm.getTagGuids() == null || !nm.getTagGuids().contains(guid)) {
                System.out.println("1 adding tags for " + nm.getTitle() + ":" + tag);
                missingGuids.add(guid);
            }
        }

        if (missingGuids.size() > 0) {
            for (String guid : missingGuids) {
                if (guid != null) {
                    note.addToTagGuids(guid);
                } else {
                    System.out.println("Warning: guid was null:");
                    new Throwable().printStackTrace(System.out);
                }
            }
        }
    }


    private void setTagsFromEMails(NoteMetadata nm, Note note) {
        if (
                nm.getAttributes() != null &&
                        nm.getAttributes().getSource() != null &&
                        nm.getAttributes().getSource().equals("mail.smtp") &&
                        nm.getAttributes().getAuthor() != null
        ) {
            System.out.println("tags for " + nm.getTitle());
            List<String> missingTags = EMailToTags.getSingleton().getTagsForEMail(nm.getAttributes().getAuthor());
            setTagsFromTagnames(nm, note, missingTags);
        }
    }

    private void setTagsFromKeywords(Note note, NoteMetadata nm, String line) {
        List<String> missingTags = new ArrayList<>();

        missingTags.addAll(KeywordsToTags.getSingleton().getTagsForKeyword(line));

        setTagsFromTagnames(nm, note, missingTags);
    }

    private boolean setTitleForPaperDocuments(NoteMetadata nm, Note note) {

        String source = "mobile.iphone";
        String title = "Foto";
        int year = LocalDate.now().getYear();


        boolean noteUpdated = false;
        if (
                nm.getAttributes() != null &&
                (nm.getAttributes().getSource() == null || source.equals(nm.getAttributes().getSource())) &&
                (nm.getTitle() == null || nm.getTitle().equals(title) || nm.getTitle().equals(""))) {
            System.out.println("----------------- reco -------------------");
            try {
                int rn = 0;
                if (
                        note.getResources() != null &&
                        note.getResources().size() > 0
                ) {

                    Resource r = note.getResources().get(0);

                    rn++;
                    BufferedImage top = null;
                    try {
                        BufferedImage img = ImageIO.read(new ByteArrayInputStream(r.getData().getBody()));
                        if (img != null && img.getWidth() > 1000 && img.getHeight() > 1000) {
                            top = img.getSubimage(img.getWidth() / 3 * 2, 0, img.getWidth() / 3, (img.getHeight() / 30));
                        }

                    } catch (Exception e) {
                    }
                    if (top != null) {
                        // this is used
                        try {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ImageIO.write(top, "png", baos);          // Format wählen
                            baos.flush();
                            String json = ServicesClient.getSingleton().ocr(baos.toByteArray());
                            System.out.println("trocr:" + json);
                            ObjectMapper om = new ObjectMapper();
                            String text = om.readTree(json).path("text").asText();
                            text = text.replaceAll("\\s+", "");
                            text = text.replace(',', '.');
                            if (
                                    text.startsWith(year + ".") ||
                                    text.startsWith((year - 1) + ".")
                            ) {
                                while (text.endsWith(".")) {
                                    text = text.substring(0, text.length() - 1);
                                }
                                if (Character.isDigit(text.charAt(text.length() - 1)) || text.charAt(text.length() - 1) == ')') {
                                    if (text.length() > 8) {
                                        // change title
                                        note.setTitle(text);
                                        noteUpdated = true;
                                    }
                                }
                            }
                            System.out.println("text:" + text);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return noteUpdated;
    }


}