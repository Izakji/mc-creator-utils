package com.izakji.agent.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ToolRegistryTest {

    @Test
    void rejectsDuplicateNamesAndSortsSchemas() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(PermissionTestSupport.readOnlyTool());
        assertEquals("read", registry.schemas().get(0).name());
        assertThrows(
                IllegalArgumentException.class,
                () -> registry.register(PermissionTestSupport.readOnlyTool()));
    }
}
