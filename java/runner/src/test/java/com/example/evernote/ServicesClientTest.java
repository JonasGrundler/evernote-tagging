package com.example.evernote;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServicesClientTest {

    private static ServicesClient client;
    private static final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void setUp() {
        client = ServicesClient.getSingleton();
        assertNotNull(client, "ServicesClient singleton should not be null – Konstruktor hat vermutlich geworfen.");
    }

    @Test
    @DisplayName("infer(): Liefert eine Tags-Liste (oder leere Liste), aber kein Null")
    void testInferScenario0() throws Exception {
        // Pfad zu einer Test-CSV (die dein Python-Service kennt)
        // -> hier entweder hart codieren oder per System-Property setzen:
        String csvPath = Paths.get(LocalStore.getSingleton().getInternet_single_notes().toString(), "d0f2188c-8ba1-4cce-b386-6b8c9cfc9d61.csv").toString();

        List<String> tags = client.infer(2, csvPath);
        assertNotNull(tags, "Tags-Liste sollte nie null sein");

        List<String> expected = new ArrayList<>();
        expected.add("PayPal");
        assertIterableEquals(expected, tags);
        // Optional:
        // assertFalse(tags.isEmpty(), "Tags-Liste sollte für dieses Test-CSV nicht leer sein");
        System.out.println("Infer tags: " + tags);
    }

    @Test
    @DisplayName("train(): Szenario 2 sollte ohne Exception durchlaufen")
    void testTrainScenario2() {
        assertDoesNotThrow(() -> client.train(2));
    }

    @Test
    @DisplayName("trocr: Gibt Ergebnis als JSON zurück")
    void testOcr() throws Exception {
        // kleines Testbild in src/test/resources/testdata/ocr_sample.png anlegen
        Path imagePath = Paths.get(LocalStore.getSingleton().getImages_tmp().toString(), "4.png");
        Assumptions.assumeTrue(Files.exists(imagePath),
                "Testbild fehlt: " + imagePath.toAbsolutePath());

        byte[] bytes = Files.readAllBytes(imagePath);

        String response = client.ocr(bytes);
        assertNotNull(response, "OCR-Response sollte nicht null sein");

        JsonNode root = mapper.readTree(response);
        assertTrue(root.has("text"), "Response-JSON sollte ein Feld 'text' enthalten");
        String text = root.get("text").asText();
        System.out.println("OCR text: " + text);
        assertEquals("2015,0001", text);
    }

    @Test
    @DisplayName("ocrImages(): POST mit images>0 läuft ohne Fehler")
    void testOcrImages() {
        assertDoesNotThrow(() -> client.ocrImages(1));
    }
}
