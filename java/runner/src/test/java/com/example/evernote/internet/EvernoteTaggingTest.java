package com.example.evernote.internet;

import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteMetadata;
import com.evernote.edam.notestore.NotesMetadataList;
import com.evernote.edam.notestore.NotesMetadataResultSpec;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.NoteAttributes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class EvernoteTaggingTest {

    /**
     * 1.0 Aufräumen (Notebooks leeren)
     * 1.5 ET-Configuration für Test anlegen
     * 2.0 Notes in Inbox einstellen
     * 3.0 EnernoteTagging laufen lassen, Test: Notes nach Tagging verschoben, Tags überprüfen, prüfen ob Training läuft
     * 4.0 Notes von Tagging in Target verschieben
     * 5.0 EvernoteTagging laufen lassen
     */

    public static final String ABLAGE = System.getenv().getOrDefault("TEST_ABLAGE", "98 Ablage");
    private static EvernoteTagging ET;
    private static Map.Entry<String, String> FROM_TO_ENTRY;

    @BeforeAll
    static void setUp() {
        System.out.println("set up");
        boolean initialized = false;
        try {
            ET = new EvernoteTagging(false);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("Exception when initializing.");
        }

        System.out.println("clean folder");
        // leeren der Notebooks (nicht: ABLAGE)
        Set<Map.Entry<String, String>> fromTo = FromToNotebooks.getSingleton().getNamePairs().entrySet();
        assertFalse(fromTo.isEmpty(), "No From-To-Notebook Mapping found.");
        String toGuid = RemoteNotebookStore.getSingleton().getGuid(ABLAGE);
        assertNotNull(toGuid, "Notebook not found:" + ABLAGE);
        FROM_TO_ENTRY = fromTo.iterator().next();
        moveNotes(FROM_TO_ENTRY.getKey(), ABLAGE);
        moveNotes(FROM_TO_ENTRY.getValue(), ABLAGE);

        System.out.println("create note");
        // erstellen der Testnotes
        Map.Entry<String, String> fromToEntry = fromTo.iterator().next();
        createNotes(fromToEntry.getKey());

        System.out.println("setup done");
    }

    private static void moveNotes(String from, String to) {
        System.out.println("move from " + from + " to " + to);
        try {
            NoteFilter f = new NoteFilter();
            f.setNotebookGuid(RemoteNotebookStore.getSingleton().getGuid(from));

            NotesMetadataResultSpec spec = new NotesMetadataResultSpec();

            NotesMetadataList meta = RemoteNoteStore.getSingleton().getNoteStore().findNotesMetadata(f, 0, 100, spec);
            // → liefert nur Notizen aus genau diesem Notizbuch (per Name gefiltert)

            List<NoteMetadata> l = meta.getNotes();

            System.out.println("found:" + l.size());

            String targetNotebookGuid = RemoteNotebookStore.getSingleton().getGuid(to);
            for (NoteMetadata nm : l) {
                Note note = RemoteNoteStore.getSingleton().getNoteStore().getNote(nm.getGuid(), true, true, true, true);

                // move
                note.setNotebookGuid(targetNotebookGuid);

                note = RemoteNoteStore.getSingleton().getNoteStore().updateNote(note);

            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("Could not move note.");
        }
    }

    private int getNotCount(String notebook) {
        try {
            NoteFilter f = new NoteFilter();
            f.setNotebookGuid(RemoteNotebookStore.getSingleton().getGuid(notebook));

            NotesMetadataResultSpec spec = new NotesMetadataResultSpec();

            NotesMetadataList meta = RemoteNoteStore.getSingleton().getNoteStore().findNotesMetadata(f, 0, 100, spec);
            // → liefert nur Notizen aus genau diesem Notizbuch (per Name gefiltert)

            return meta.getNotes().size();

        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("Could not move note.");
        }
        return -1;
    }

    private static void createNotes(String to) {
        try {
            String targetNotebookGuid = RemoteNotebookStore.getSingleton().getGuid(to);

            InputStream is = EvernoteTaggingTest.class.getResourceAsStream("/runner/test_note_1.txt");
            assertNotNull(is);

            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            Note note = new Note();
            note.setNotebookGuid(targetNotebookGuid);
            note.setTitle("Du hast eine Zahlung gesendet");
            NoteAttributes na = new NoteAttributes();
            na.setAuthor("service@paypal.de");
            na.setSource("mail.smtp");
            note.setAttributes(na);
            note.setContent(content);

            RemoteNoteStore.getSingleton().getNoteStore().createNote(note);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("Could not create note.");
        }
    }

    @Test
    @DisplayName("testTagging(): Tagging einer Note")
    void testTagging() throws Exception {
        assertTrue(true);
        int tcount = ET.tagNotes();
        assertEquals(1, tcount);
        assertEquals(1, getNotCount(FROM_TO_ENTRY.getValue()));
        moveNotes(FROM_TO_ENTRY.getValue(), ABLAGE);
        int mcount = ET.addNotesToCSVAndTrackChanges();
        assertEquals(1, mcount);
        boolean trained = ET.train();
        assertTrue(trained);
    }

}
