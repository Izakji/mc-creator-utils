package com.izakji.agent.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class ProviderPresetsTest {

    @Test
    void deepSeekUsesOpenAiCompatibleEndpoint() {
        ProviderProfile profile = ProviderPresets.deepSeek("sk-secret");
        assertEquals(ProviderType.OPENAI_COMPATIBLE, profile.type());
        assertEquals("https://api.deepseek.com", profile.baseUrl());
        assertEquals("deepseek-chat", profile.model());
        assertFalse(profile.toString().contains("sk-secret"));
    }

    @Test
    void anthropicUsesNativeAdapter() {
        assertEquals(ProviderType.ANTHROPIC, ProviderPresets.anthropic("key").type());
    }

    @Test
    void customPresetKeepsEmptyModelForUserEntry() {
        ProviderProfile profile = ProviderPresets.custom(ProviderType.OPENAI_COMPATIBLE);
        assertEquals("", profile.model());
        assertEquals("https://api.openai.com/v1", profile.baseUrl());
    }
}
