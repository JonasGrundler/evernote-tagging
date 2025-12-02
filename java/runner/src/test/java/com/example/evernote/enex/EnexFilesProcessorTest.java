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

        // Überprüfung erfolgt per Directory-Compare

    }

}
