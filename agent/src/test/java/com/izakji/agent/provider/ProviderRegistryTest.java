package com.izakji.agent.provider;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.izakji.agent.config.ProviderType;
import org.junit.jupiter.api.Test;

class ProviderRegistryTest {

    @Test
    void looksUpProvidersAndRejectsDuplicates() {
        ProviderRegistry registry = new ProviderRegistry();
        registry.register(new OpenAiCompatibleProvider());
        assertTrue(registry.forType(ProviderType.OPENAI_COMPATIBLE).isPresent());
        assertThrows(
                IllegalArgumentException.class, () -> registry.register(new OpenAiCompatibleProvider()));
    }
}
