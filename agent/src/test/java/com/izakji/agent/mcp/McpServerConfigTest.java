package com.izakji.agent.mcp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class McpServerConfigTest {

    @Test
    void doesNotExposeHeadersOrEnvironmentInToString() {
        McpServerConfig config = new McpServerConfig(
                "server",
                "Server",
                McpTransportType.STDIO,
                "node",
                List.of(),
                null,
                Map.of("ENV_SECRET", "env-secret"),
                null,
                Map.of("Authorization", "Bearer header-secret"),
                true,
                false);
        String text = config.toString();
        assertFalse(text.contains("env-secret"));
        assertFalse(text.contains("header-secret"));
    }

    @Test
    void exposesSecretsForTheRuntimeRedactor() {
        McpServerConfig config = new McpServerConfig(
                "server",
                "Server",
                McpTransportType.STDIO,
                "node",
                List.of(),
                null,
                Map.of("ENV_SECRET", "env-secret"),
                null,
                Map.of("Authorization", "Bearer header-secret"),
                true,
                false);
        McpManager manager = new McpManager(Map.of(), Map.of("server", config));
        assertTrue(manager.secrets().containsAll(List.of("env-secret", "Bearer header-secret")));
    }
}
