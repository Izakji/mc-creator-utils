package com.izakji.mccreatorutils.client.agent.context;

import com.google.gson.JsonObject;
import com.izakji.agent.api.AgentTool;
import com.izakji.agent.api.ToolContext;
import com.izakji.agent.api.ToolInvocation;
import com.izakji.agent.api.ToolResult;
import com.izakji.agent.api.ToolRisk;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Read-only client performance history tool.
 *
 * @author Izakji
 */
public final class PerformanceTool implements AgentTool {
    private final PerformanceMonitor monitor;

    public PerformanceTool(PerformanceMonitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public String name() {
        return "performance";
    }

    @Override
    public String description() {
        return "Read recent client FPS, memory, ping, and render-distance history";
    }

    @Override
    public JsonObject inputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", new JsonObject());
        return schema;
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.READ_ONLY;
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolInvocation invocation, ToolContext context) {
        return CompletableFuture.completedFuture(
                new ToolResult(invocation.callId(), PerformanceReport.format(monitor.samples()), false, List.of()));
    }
}
