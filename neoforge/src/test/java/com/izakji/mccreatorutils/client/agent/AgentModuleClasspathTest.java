package com.izakji.mccreatorutils.client.agent;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.izakji.agent.config.AgentSettings;
import org.junit.jupiter.api.Test;

class AgentModuleClasspathTest {
    @Test
    void agentCoreIsVisibleToTheNeoForgeAdapter() {
        assertNotNull(AgentSettings.class);
    }
}
