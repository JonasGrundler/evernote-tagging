package com.example.evernote.enex;

import com.example.evernote.LocalStore;
import com.example.evernote.ServicesClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EnexFilesProcessorTest {

    @Test
    @DisplayName("EnexFilesProcessor: parsen der enex-Files")
    void testMain() throws Exception {

        EnexFilesProcessor.main(null);

        /*
        String csvPath = Paths.get(LocalStore.getSingleton().getInternet_single_notes().toString(), "d0f2188c-8ba1-4cce-b386-6b8c9cfc9d61.csv").toString();

        List<String> tags = client.infer(2, csvPath);
        assertNotNull(tags, "Tags-Liste sollte nie null sein");

        List<String> expected = new ArrayList<>();
        expected.add("PayPal");
        assertIterableEquals(expected, tags);
        // Optional:
        // assertFalse(tags.isEmpty(), "Tags-Liste sollte für dieses Test-CSV nicht leer sein");
        System.out.println("Infer tags: " + tags);

         */
    }


}
