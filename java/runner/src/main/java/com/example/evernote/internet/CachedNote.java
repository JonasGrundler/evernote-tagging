package com.example.evernote.internet;

import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteMetadata;
import com.evernote.edam.notestore.NotesMetadataList;
import com.evernote.edam.notestore.NotesMetadataResultSpec;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.Tag;
import org.apache.commons.text.StringEscapeUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class CachedNote extends CacheHelper {

    private static final long UPDATE_TIMEOUT = 12 * 60 * 60 * 1000;
    private static final String NOTEBOOK_AUTOMATION = "10 Automatisierung";

    private String noteTitle = null;
    private String noteGuid = null;

    public CachedNote(String noteTitle) {
        this.noteTitle = noteTitle;
    }

    public void init() {
        if (
                System.currentTimeMillis() - getLastUpdate() > UPDATE_TIMEOUT ||
                RemoteTagStore.getSingleton().getLastUpdate() > getLastUpdate()
        ) {
            System.out.println("init NoteToTags:" + getClass() + ":" + noteTitle);
            try {
                //
                map.clear();
                tags.clear();

                //
                if (noteGuid == null) {
                    String guid = RemoteNotebookStore.getSingleton().getNameToGuid().get(NOTEBOOK_AUTOMATION);
                    NoteFilter nf = new NoteFilter();
                    nf.setNotebookGuid(guid);
                    nf.setWords(noteTitle);

                    NotesMetadataResultSpec spec = new NotesMetadataResultSpec();
                    spec.setIncludeTitle(true);

                    NotesMetadataList results = RemoteNoteStore.getSingleton().getNoteStore().findNotesMetadata(nf, 0, 5, spec);

                    for (NoteMetadata nm : results.getNotes()) {
                        if (nm.getTitle() != null && nm.getTitle().equals(noteTitle)) {
                            noteGuid = nm.getGuid();
                            break;
                        }
                    }
                }

                Note foundNote = RemoteNoteStore.getSingleton().getNoteStore().getNote(
                        noteGuid, true, true, false, false);
                Pattern pattern = Pattern.compile("<div>(.*?)</div>");
                Matcher matcher = pattern.matcher(StringEscapeUtils.unescapeHtml4(foundNote.getContent()));

                List<String> contents = new ArrayList<>();
                while (matcher.find()) {
                    String line = matcher.group(1).trim();
                    String[] s = line.split(":");
                    if (s.length == 2) {
                        String[] s2 = s[1].split(",");
                        addInfo(s[0], s2);
                    }
                }

                // validate...
                System.out.println("mapping");
                for (String eTag : tags) {
                    boolean found = false;
                    for (Tag tag : RemoteTagStore.getSingleton().getTags()) {
                        if (tag.getName().equals(eTag)) {
                            found = true;
                            continue;
                        }
                    }
                    if (!found) {
                        System.out.println("eTag " + eTag + " not found!");
                    }
                }
                store();
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }
    }

    private void addInfo (String key, String... tags) {
        add(key, tags);
    }

    private void add (String key, String[] eTags) {
        List<String> l = new ArrayList<>();
        map.put(key, l);
        for (String tag : eTags) {
            String t = tag.trim();
            if (t.length() > 0) {
                l.add(t);
                tags.add(t);
            }
        }
    }

    public Set<String> getAllTags () {
        init();
        return tags;
    }

    public List<String> getTagsForKey (String key) {
        init();
        return map.get(key);
    }

    public Set<String> getAllKeys() {
        init();
        return map.keySet();
    }

}
