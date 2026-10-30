package com.izakji.agent.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.izakji.agent.api.AgentException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AgentSettingsStoreTest {

    @Test
    void roundTripsSettingsAndRedactsKeyFromErrors() throws Exception {
        Path dir = Files.createTempDirectory("agent-settings");
        AgentSettingsStore store = new AgentSettingsStore();
        AgentSettings settings = TestSettings.settings();
        store.save(dir.resolve("agent.json"), settings);
        AgentSettings loaded = store.load(dir.resolve("agent.json"));
        assertEquals("main", loaded.defaultProfileId());
        assertEquals(1, loaded.profiles().size());
        assertEquals(8, loaded.maxToolRounds());
    }

    @Test
    void rejectsMalformedJsonWithoutEchoingFileContents() throws Exception {
        Path file = Files.createTempFile("agent-settings", ".json");
        Files.writeString(file, "{\"apiKey\":\"sk-secret\"");
        AgentException error = assertThrows(
                AgentException.class, () -> new AgentSettingsStore().load(file));
        assertFalse(error.getMessage().contains("sk-secret"));
    }
}
