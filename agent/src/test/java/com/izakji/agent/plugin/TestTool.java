package com.izakji.agent.plugin;

import com.google.gson.JsonObject;
import com.izakji.agent.api.AgentTool;
import com.izakji.agent.api.ToolContext;
import com.izakji.agent.api.ToolInvocation;
import com.izakji.agent.api.ToolResult;
import com.izakji.agent.api.ToolRisk;
import java.util.List;
import java.util.concurrent.CompletableFuture;

final class TestTool implements AgentTool {
    private final String name;

    TestTool(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return name;
    }

    @Override
    public JsonObject inputSchema() {
        return new JsonObject();
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.READ_ONLY;
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolInvocation invocation, ToolContext context) {
        return CompletableFuture.completedFuture(new ToolResult(invocation.callId(), "", false, List.of()));
    }
}
