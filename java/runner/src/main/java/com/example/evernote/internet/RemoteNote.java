package com.example.evernote.internet;

import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteMetadata;
import com.evernote.edam.notestore.NotesMetadataList;
import com.evernote.edam.notestore.NotesMetadataResultSpec;
import com.evernote.edam.type.Note;

import java.util.*;

public abstract class RemoteNote extends CacheHelper {

    private static final long UPDATE_TIMEOUT = 12 * 60 * 60 * 1000;
    public static final String EN_USER_CONF_NB = System.getenv().getOrDefault("EN_USER_CONF_NB", "10 Automatisierung");

    private final String noteTitle;
    private String noteGuid = null;

    public RemoteNote(String noteTitle) {
        this.noteTitle = noteTitle;
    }

    public void init() {
        if (
                System.currentTimeMillis() - getLastUpdate() > UPDATE_TIMEOUT ||
                RemoteTagStore.getSingleton().getLastUpdate() > getLastUpdate()
        ) {
            System.out.println("init RemoteNote:" + getClass() + ":" + noteTitle);
            try {

                //
                if (noteGuid == null) {
                    String guid = RemoteNotebookStore.getSingleton().getNameToGuid().get(EN_USER_CONF_NB);
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
                prepare(foundNote);
                store();
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }
    }

    abstract void prepare (Note foundNote);

}
