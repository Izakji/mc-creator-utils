package com.izakji.mccreatorutils.client.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.izakji.agent.config.ProviderProfile;
import com.izakji.agent.config.ProviderType;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AgentProviderDraftTest {

    @Test
    void blankRequiredFieldsProduceValidationErrors() {
        AgentProviderDraft draft =
                new AgentProviderDraft("", ProviderType.OPENAI_COMPATIBLE, "", "", "", 0.7, 2048, 128000, true, 0);
        assertTrue(draft.validationErrors().contains("Display name is required"));
        assertTrue(draft.validationErrors().contains("Base URL is required"));
        assertTrue(draft.validationErrors().contains("Model is required"));
    }

    @Test
    void blankKeyKeepsTheExistingKeyAndNeverExposesIt() {
        ProviderProfile existing = new ProviderProfile(
                "openai",
                "OpenAI",
                ProviderType.OPENAI_COMPATIBLE,
                "https://api.openai.com/v1",
                "gpt-4o-mini",
                "sk-existing",
                Map.of(),
                0.7,
                2048,
                128000,
                true,
                true);
        AgentProviderDraft draft = AgentProviderDraft.from(existing);
        assertEquals("", draft.apiKey());
        assertEquals("sk-existing", draft.toProfile("openai", "sk-existing").apiKey());
        assertEquals(
                "sk-new",
                draft.withApiKey("sk-new").toProfile("openai", "sk-existing").apiKey());
    }

    @Test
    void toStringRedactsKeysAndHeaders() {
        AgentProviderDraft draft = new AgentProviderDraft(
                        "OpenAI",
                        ProviderType.OPENAI_COMPATIBLE,
                        "https://api.openai.com/v1",
                        "gpt-4o-mini",
                        "sk-secret",
                        0.7,
                        2048,
                        128000,
                        true,
                        0,
                        Map.of("Authorization", "Bearer header-secret"))
                .withApiKey("sk-secret");
        String text = draft.toString();
        assertTrue(!text.contains("sk-secret"));
        assertTrue(!text.contains("header-secret"));
    }
}
