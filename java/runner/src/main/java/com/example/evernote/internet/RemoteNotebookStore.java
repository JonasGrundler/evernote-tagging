package com.example.evernote.internet;

import com.evernote.edam.type.Notebook;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class RemoteNotebookStore extends CacheHelper {

    private Map<String, String> nameToGuid = new HashMap<>();
    private Map<String, String> guidToName = new HashMap<>();

    private static final long UPDATE_TIMEOUT = 12 * 60 * 60 * 1000;

    private static final RemoteNotebookStore singleton = CacheHelper.load(RemoteNotebookStore.class);

    public static RemoteNotebookStore getSingleton() {
        return singleton;
    }

    public RemoteNotebookStore() {
    }

    private void init() {
        if (System.currentTimeMillis() - getLastUpdate() > UPDATE_TIMEOUT) {
            nameToGuid.clear();
            guidToName.clear();
            try {
                for (Notebook nb : RemoteNoteStore.getSingleton().getNoteStore().listNotebooks()) {
                    guidToName.put(nb.getGuid(), nb.getName());
                    nameToGuid.put(nb.getName(), nb.getGuid());
                }
                store();
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }
    }

    public String getName(String guid) {
        init();
        return guidToName.get(guid);
    }

    public String getGuid(String name) {
        init();
        return nameToGuid.get(name);
    }

    public Map<String, String> getNameToGuid() {
        init();
        return nameToGuid;
    }
}
