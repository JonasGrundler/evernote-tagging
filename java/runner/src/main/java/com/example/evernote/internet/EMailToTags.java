package com.example.evernote.internet;

import java.util.*;


public class EMailToTags extends NoteToTags {

    private static final String NOTE_EMAIL_TO_TAGS = "EMailToTags";

    private static final EMailToTags singleton = CacheHelper.load(EMailToTags.class);

    public static EMailToTags getSingleton() {
        return singleton;
    }

    public static void main(String[] args) {
        try {
            System.out.println("tags:" + EMailToTags.getSingleton().getTagsForKey("APCOA"));
            System.out.println("RemoteTagStore:" + RemoteTagStore.getSingleton().getTags());
            System.out.println("EMailToTags:" + EMailToTags.getSingleton().getAllKeys());
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    public EMailToTags () {
        super (NOTE_EMAIL_TO_TAGS);
    }

    public List<String> getTagsForEMail (String text) {
        init();
        List<String> tags = new ArrayList<>();
        for (String key : getAllKeys()) {
            if (text.endsWith(key)) {
                tags.addAll(getTagsForKey(key));
            }
        }
        return tags;
    }

}
