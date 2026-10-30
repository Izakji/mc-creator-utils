package com.izakji.agent.plugin;

import static com.izakji.agent.plugin.PluginTestSupport.plugin;
import static com.izakji.agent.plugin.PluginTestSupport.tool;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.izakji.agent.api.AgentException;
import org.junit.jupiter.api.Test;

class AgentPluginRegistryTest {

    @Test
    void collectsToolsAndRejectsDuplicateNames() throws Exception {
        AgentPluginRegistry registry = new AgentPluginRegistry();
        registry.register(plugin("a", context -> context.registerTool(tool("one"))));
        assertTrue(registry.tools().find("one").isPresent());
        assertThrows(
                AgentException.class,
                () -> registry.register(plugin("b", context -> context.registerTool(tool("one")))));
    }
}
