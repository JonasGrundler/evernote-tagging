package com.example.evernote.internet;

import com.evernote.edam.type.Tag;
import org.apache.commons.collections4.list.TreeList;
import org.apache.pdfbox.debugger.ui.Tree;

import java.util.*;

public class RemoteTagStore extends CacheHelper {

    private List<Tag> tags = new TreeList<>();
    private Map<String, String> nameToGuid = new HashMap<>();
    private Map<String, String> guidToName = new HashMap<>();

    private static final long UPDATE_TIMEOUT = 12 * 60 * 60 * 1000;

    private static final RemoteTagStore singleton = load(RemoteTagStore.class);

    public static RemoteTagStore getSingleton() {
        return singleton;
    }

    public RemoteTagStore() {
    }

    public void init() {
        init(false);
    }

    private void init(boolean force) {
        if (force || System.currentTimeMillis() - getLastUpdate() > UPDATE_TIMEOUT) {
            try {
                tags.clear();
                nameToGuid.clear();
                guidToName.clear();

                tags.addAll(RemoteNoteStore.getSingleton().getNoteStore().listTags());
                for (Tag tag : tags) {
                    System.out.println("RemoteTagStore:" + tag.getName() + " - " + tag.getGuid());
                    nameToGuid.put(tag.getName(), tag.getGuid());
                    guidToName.put(tag.getGuid(), tag.getName());
                }
                store();
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }
    }

    public List<Tag> getTags() {
        init();
        return tags;
    }

    public String getGuidFromName(String name) {
        init();
        String guid = nameToGuid.get(name);
        if (guid == null) {
            init(true);
            guid = nameToGuid.get(name);
        }
        return guid;
    }

    public String getNameFromGuid(String guid) {
        init();
        String name = guidToName.get(guid);
        if (name == null) {
            init(true);
            name = guidToName.get(guid);
        }
        return name;
    }

}
