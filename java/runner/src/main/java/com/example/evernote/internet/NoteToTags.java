package com.example.evernote.internet;

import com.evernote.edam.type.Note;
import com.evernote.edam.type.Tag;
import org.apache.commons.text.StringEscapeUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class NoteToTags extends RemoteNote {

    private Map<String, List<String>> map = new HashMap<>();
    private Set<String> tags = new HashSet<>();

    public NoteToTags(String noteTitle) {
        super (noteTitle);
    }

    void prepare(Note foundNote) {
        try {
            //
            map.clear();
            tags.clear();

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

    private void addInfo (String key, String... tags) {
        add(key, tags);
    }

    private void add (String key, String[] eTags) {
        List<String> l = new ArrayList<>();
        map.put(key, l);
        for (String tag : eTags) {
            String t = tag.trim();
            if (!t.isEmpty()) {
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
