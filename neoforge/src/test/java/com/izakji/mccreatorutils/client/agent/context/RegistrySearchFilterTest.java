package com.izakji.mccreatorutils.client.agent.context;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RegistrySearchFilterTest {

    @Test
    void matchesIdPathAndNamespace() {
        assertTrue(RegistrySearchFilter.matches("minecraft:plains", "plain", ""));
        assertTrue(RegistrySearchFilter.matches("minecraft:plains", "", "minecraft"));
        assertFalse(RegistrySearchFilter.matches("minecraft:plains", "desert", ""));
        assertFalse(RegistrySearchFilter.matches("minecraft:plains", "", "terralith"));
    }
}
