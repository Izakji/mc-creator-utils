package com.izakji.agent.runtime;

import com.google.gson.JsonObject;
import com.izakji.agent.api.AgentTool;
import com.izakji.agent.api.ToolContext;
import com.izakji.agent.api.ToolInvocation;
import com.izakji.agent.api.ToolResult;
import com.izakji.agent.api.ToolRisk;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

final class FakeTool implements AgentTool {
    private final String name;
    private final ToolRisk risk;
    private final String result;
    final List<ToolInvocation> executions = new ArrayList<>();
    private RuntimeException failure;

    FakeTool(String name, ToolRisk risk, String result) {
        this.name = name;
        this.risk = risk;
        this.result = result;
    }

    void failWith(RuntimeException failure) {
        this.failure = failure;
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
        return risk;
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolInvocation invocation, ToolContext context) {
        executions.add(invocation);
        if (failure != null) {
            return CompletableFuture.failedFuture(failure);
        }
        return CompletableFuture.completedFuture(
                new ToolResult(invocation.callId(), result, false, List.of()));
    }
}
