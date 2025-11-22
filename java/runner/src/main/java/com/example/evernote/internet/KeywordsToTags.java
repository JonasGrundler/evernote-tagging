package com.example.evernote.internet;

import java.util.*;


public class KeywordsToTags extends NoteToTags {

    private static final String NOTE_KEYWORDS_TO_TAGS = "KeywordsToTags";

    private static final KeywordsToTags singleton = CacheHelper.load(KeywordsToTags.class);

    public static KeywordsToTags getSingleton() {
        return singleton;
    }

    public static void main(String[] args) {
        try {
            System.out.println("tags:" + KeywordsToTags.getSingleton().getTagsForKey("APCOA"));
            System.out.println("RemoteTagStore:" + RemoteTagStore.getSingleton().getTags());
            System.out.println("KeywordsToTags:" + KeywordsToTags.getSingleton().getAllKeys());
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    public KeywordsToTags () {
        super (NOTE_KEYWORDS_TO_TAGS);
    }

    public List<String> getTagsForKeyword (String text) {
        init();
        List<String> tags = new ArrayList<>();
        for (String key : getAllKeys()) {
            if (text.contains(key)) {
                tags.addAll(getTagsForKey(key));
            }
        }
        return tags;
    }

}