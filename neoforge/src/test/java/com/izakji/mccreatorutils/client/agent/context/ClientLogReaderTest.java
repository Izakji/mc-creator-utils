package com.izakji.mccreatorutils.client.agent.context;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ClientLogReaderTest {

    @Test
    void tailsTheNewestLines() throws Exception {
        Path file = Files.createTempFile("agent-log", ".log");
        Files.writeString(file, "old\nwarning one\nERROR boom\n");
        ClientLogReader reader = new ClientLogReader(file, Runnable::run);
        String text = reader.tail(2).join();
        assertTrue(text.contains("warning one"));
        assertTrue(text.contains("ERROR boom"));
        assertFalse(text.contains("old"));
    }

    @Test
    void searchesCaseInsensitively() throws Exception {
        Path file = Files.createTempFile("agent-log", ".log");
        Files.writeString(file, "info\nERROR boom\nwarning\n");
        ClientLogReader reader = new ClientLogReader(file, Runnable::run);
        String text = reader.search("error", 10).join();
        assertTrue(text.contains("ERROR boom"));
        assertFalse(text.contains("warning"));
    }

    @Test
    void reportsMissingLog() {
        ClientLogReader reader = new ClientLogReader(Path.of("missing-agent-log.log"), Runnable::run);
        assertTrue(reader.tail(10).join().contains("No client log"));
    }
}
