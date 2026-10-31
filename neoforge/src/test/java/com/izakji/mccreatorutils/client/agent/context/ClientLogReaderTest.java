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
    void filtersByLevelAndAddsContext() throws Exception {
        Path file = Files.createTempFile("agent-log", ".log");
        Files.writeString(file, "before\n[12:00:00] [Render thread/ERROR] [com.foo/]: boom\nafter\n");
        ClientLogReader reader = new ClientLogReader(file, Runnable::run);
        String text = reader.search("boom", 10, "ERROR", 1).join();
        assertTrue(text.contains("> [12:00:00] [Render thread/ERROR]"));
        assertTrue(text.contains("  before"));
        assertTrue(text.contains("  after"));
        assertTrue(reader.search("boom", 10, "WARN", 0).join().contains("No matching log lines"));
        assertTrue(reader.search("boom", 10, "BOGUS", 0).join().contains("Unknown log level"));
    }

    @Test
    void summarizesLevelsAndGroups() throws Exception {
        Path file = Files.createTempFile("agent-log", ".log");
        Files.writeString(
                file,
                "[12:00:00] [Render thread/WARN] [com.foo/]: slow 1\n"
                        + "[12:00:01] [Render thread/WARN] [com.foo/]: slow 2\n"
                        + "[12:00:02] [Render thread/ERROR] [com.foo/]: boom\n");
        ClientLogReader reader = new ClientLogReader(file, Runnable::run);
        String text = reader.summary(10).join();
        assertTrue(text.contains("ERROR 1, WARN 2"));
        assertTrue(text.contains("2x WARN: slow #"));
    }

    @Test
    void reportsMissingLog() {
        ClientLogReader reader = new ClientLogReader(Path.of("missing-agent-log.log"), Runnable::run);
        assertTrue(reader.tail(10).join().contains("No client log"));
    }
}
