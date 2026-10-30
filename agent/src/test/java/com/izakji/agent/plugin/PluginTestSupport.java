package com.izakji.agent.plugin;

import java.util.function.Consumer;

final class PluginTestSupport {
    private PluginTestSupport() {}

    static TestTool tool(String name) {
        return new TestTool(name);
    }

    static TestPlugin plugin(String id, Consumer<AgentPluginContext> body) {
        return new TestPlugin(id, body);
    }
}
