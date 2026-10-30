package com.izakji.agent.plugin;

import java.util.function.Consumer;

final class TestPlugin implements AgentPlugin {
    private final String id;
    private final Consumer<AgentPluginContext> body;

    TestPlugin(String id, Consumer<AgentPluginContext> body) {
        this.id = id;
        this.body = body;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void register(AgentPluginContext context) {
        body.accept(context);
    }
}
