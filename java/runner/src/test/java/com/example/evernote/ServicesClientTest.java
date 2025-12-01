package com.example.evernote;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import java.nio.file.Files;
import java.nio.file.Path;
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
    @DisplayName("train(): Szenario 0 sollte ohne Exception durchlaufen")
    void testTrainScenario0() {
        assertDoesNotThrow(() -> client.train(0));
    }

    @Test
    @DisplayName("train(): Szenario 0 sollte ohne Exception durchlaufen")
    void testTrainScenario1() {
        assertDoesNotThrow(() -> client.train(1));
    }

    @Test
    @DisplayName("train(): Szenario 0 sollte ohne Exception durchlaufen")
    void testTrainScenario2() {
        assertDoesNotThrow(() -> client.train(2));
    }

    @Test
    @DisplayName("infer(): Liefert eine Tags-Liste (oder leere Liste), aber kein Null")
    void testInferScenario0() throws Exception {
        // Pfad zu einer Test-CSV (die dein Python-Service kennt)
        // -> hier entweder hart codieren oder per System-Property setzen:
        String csvPath = System.getProperty("test.csvPath");

        Assumptions.assumeTrue(csvPath != null && !csvPath.isBlank(),
                "System Property 'test.csvPath' muss gesetzt sein, z.B. -Dtest.csvPath=/pfad/zu/datei.csv");

        List<String> tags = client.infer(0, csvPath);
        assertNotNull(tags, "Tags-Liste sollte nie null sein");
        // Optional:
        // assertFalse(tags.isEmpty(), "Tags-Liste sollte für dieses Test-CSV nicht leer sein");
        System.out.println("Infer tags: " + tags);
    }

    @Test
    @DisplayName("ocr(): Gibt JSON mit Text-Feld zurück")
    void testOcr() throws Exception {
        // kleines Testbild in src/test/resources/testdata/ocr_sample.png anlegen
        Path imagePath = Path.of("src/test/resources/testdata/ocr_sample.png");
        Assumptions.assumeTrue(Files.exists(imagePath),
                "Testbild fehlt: " + imagePath.toAbsolutePath());

        byte[] bytes = Files.readAllBytes(imagePath);

        String response = client.ocr(bytes);
        assertNotNull(response, "OCR-Response sollte nicht null sein");

        JsonNode root = mapper.readTree(response);
        assertTrue(root.has("text"), "Response-JSON sollte ein Feld 'text' enthalten");
        System.out.println("OCR text: " + root.get("text").asText());
    }

    @Test
    @DisplayName("ocrImages(): POST mit images>0 läuft ohne Fehler")
    void testOcrImages() {
        assertDoesNotThrow(() -> client.ocrImages(1));
    }
}
