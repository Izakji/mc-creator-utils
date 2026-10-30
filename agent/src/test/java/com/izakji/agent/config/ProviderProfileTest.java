package com.izakji.agent.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ProviderProfileTest {

    @Test
    void rejectsBlankRequiredFields() {
        assertThrows(IllegalArgumentException.class, () -> profile("", "m", "https://x"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ProviderProfile(
                        "id",
                        " ",
                        ProviderType.OPENAI_COMPATIBLE,
                        "https://x",
                        "m",
                        "sk-secret",
                        Map.of(),
                        0.7,
                        2048,
                        128000,
                        true,
                        true));
        assertThrows(IllegalArgumentException.class, () -> profile("id", "m", " "));
    }

    @Test
    void redactsApiKeyFromToString() {
        ProviderProfile profile = profile("main", "model", "https://api.example.com");
        assertFalse(profile.toString().contains("sk-secret"));
        assertTrue(profile.toString().contains("main"));
    }

    private static ProviderProfile profile(String id, String model, String url) {
        return new ProviderProfile(
                id,
                id,
                ProviderType.OPENAI_COMPATIBLE,
                url,
                model,
                "sk-secret",
                Map.of(),
                0.7,
                2048,
                128000,
                true,
                true);
    }
}
