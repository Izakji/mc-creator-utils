package com.izakji.mccreatorutils.client.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.izakji.agent.config.AgentSettings;
import com.izakji.agent.config.ProviderProfile;
import com.izakji.agent.config.ProviderType;
import com.izakji.agent.runtime.AgentRuntime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AgentClientServiceTest {
    @Test
    void createsAgentDirectoryLayout() throws Exception {
        Path root = Files.createTempDirectory("agent-config");
        AgentPaths paths = AgentPaths.at(root);
        paths.ensureDirectories();
        assertEquals(root.resolve("mccreatorutils/agent"), paths.root());
        assertTrue(Files.isDirectory(paths.skillsDir()));
        assertTrue(Files.isDirectory(paths.packsDir()));
        assertTrue(Files.isDirectory(paths.conversationsDir()));
    }

    @Test
    void createsDefaultSettingsFile() throws Exception {
        Path root = Files.createTempDirectory("agent-config");
        try (AgentClientService service = AgentClientService.forConfig(root)) {
            assertEquals("openai", service.settings().defaultProfileId());
            assertEquals("OpenAI", service.settings().profiles().get(0).displayName());
            assertTrue(Files.isRegularFile(AgentPaths.at(root).settingsFile()));
            assertNotNull(service.runtime());
        }
    }

    @Test
    void savesSettingsAndRebuildsTheRuntime() throws Exception {
        Path root = Files.createTempDirectory("agent-config");
        try (AgentClientService service = AgentClientService.forConfig(root)) {
            AgentSettings changed = new AgentSettings(
                    "openai",
                    List.of(new ProviderProfile(
                            "openai",
                            "My OpenAI",
                            ProviderType.OPENAI_COMPATIBLE,
                            "https://api.openai.com/v1",
                            "gpt-4o-mini",
                            "",
                            Map.of(),
                            0.5,
                            4096,
                            128000,
                            true,
                            true)),
                    service.settings().permissions(),
                    8,
                    32,
                    60);
            AgentRuntime first = service.runtime();
            service.saveSettings(changed);
            assertEquals("My OpenAI", service.settings().profiles().get(0).displayName());
            assertNotSame(first, service.runtime());
        }
    }

    @Test
    void survivesCorruptConversationWhenListing() throws Exception {
        Path root = Files.createTempDirectory("agent-config");
        try (AgentClientService service = AgentClientService.forConfig(root)) {
            String id = java.util.UUID.randomUUID().toString();
            Files.writeString(AgentPaths.at(root).conversationsDir().resolve(id + ".json"), "{broken");
            assertTrue(service.listConversations().isEmpty());
            assertTrue(Files.list(AgentPaths.at(root).conversationsDir())
                    .anyMatch(path -> path.getFileName().toString().contains(".corrupt-")));
        }
    }
}
