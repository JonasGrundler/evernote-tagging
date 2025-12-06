package com.example.evernote.internet;

import com.evernote.edam.type.Note;
import org.apache.commons.text.StringEscapeUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FromToNotebooks extends RemoteNote {

    private static final String NOTEBOOKS_TO_TAGS = "Notebooks";

    private static final FromToNotebooks singleton = CacheHelper.load(FromToNotebooks.class);

    private final Map<String, String> nameMap = new HashMap<>();

    public static FromToNotebooks getSingleton() {
        return singleton;
    }

    public FromToNotebooks() {
        super (NOTEBOOKS_TO_TAGS);
    }

    public Map<String, String> getNamePairs () {
        init();
        return nameMap;
    }

    @Override
    void prepare(Note foundNote) {
        try {
            //
            Pattern pattern = Pattern.compile("<div>(.*?)</div>");
            Matcher matcher = pattern.matcher(StringEscapeUtils.unescapeHtml4(foundNote.getContent()));

            Map<String, String> mapTmp = new HashMap<>();
            while (matcher.find()) {
                String line = matcher.group(1).trim();

                int fromIdx = line.indexOf("from:");

                if (fromIdx >= 0) {
                    int toIdx = line.indexOf("to:");

                    if (toIdx >= 0) {

                        String from = line.substring(fromIdx + "from:".length(), toIdx).trim();
                        String to = line.substring(toIdx + "to:".length()).trim();

                        mapTmp.put(from, to);
                    }
                }
            }

            // validate...
            System.out.println("validate notebooks");
            nameMap.clear();
            String fromGuid;
            String toGuid;
            for (Map.Entry<String, String> e : mapTmp.entrySet()) {
                fromGuid = RemoteNotebookStore.getSingleton().getGuid(e.getKey());
                if (fromGuid != null) {
                    toGuid = RemoteNotebookStore.getSingleton().getGuid(e.getValue());
                    if (toGuid != null) {
                        nameMap.put(e.getKey(), e.getValue());
                    } else {
                        System.out.println("To-Notebook " + e.getValue() + " not found!");
                    }
                } else {
                    System.out.println("From-Notebook " + e.getKey() + " not found!");
                }
            }
            store();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

    }
}
