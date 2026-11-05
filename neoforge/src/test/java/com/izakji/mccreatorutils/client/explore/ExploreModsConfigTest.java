package com.izakji.mccreatorutils.client.explore;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ExploreModsConfigTest {

    @Test
    void environmentTakesPrecedenceAndIsTrimmed() {
        assertEquals("env-key", ExploreModsSecrets.resolve("  env-key  ", "config-key"));
    }

    @Test
    void blankEnvironmentFallsBackToConfig() {
        assertEquals("config-key", ExploreModsSecrets.resolve("", "  config-key  "));
        assertEquals("config-key", ExploreModsSecrets.resolve(null, "config-key"));
    }

    @Test
    void blankValuesResolveToEmpty() {
        assertEquals("", ExploreModsSecrets.resolve("", ""));
        assertEquals("", ExploreModsSecrets.resolve(null, null));
    }
}
