package com.izakji.agent.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TestSettings {
    private TestSettings() {}

    public static AgentSettings settings() {
        ProviderProfile profile = new ProviderProfile(
                "main",
                "Test",
                ProviderType.OPENAI_COMPATIBLE,
                "https://example.invalid/v1",
                "test-model",
                "sk-secret",
                Map.of(),
                0.7,
                2048,
                128000,
                true,
                true);
        return new AgentSettings(
                "main", List.of(profile), new PermissionRules(true, Set.of(), Set.of(), Set.of()), 8, 32, 1);
    }
}
