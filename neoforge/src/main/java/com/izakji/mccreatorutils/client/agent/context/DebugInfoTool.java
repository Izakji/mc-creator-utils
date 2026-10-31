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
 * Read-only F3 debug and exact JVM/GC/OS information tool.
 *
 * @author Izakji
 */
public final class DebugInfoTool implements AgentTool {

    @Override
    public String name() {
        return "debug_info";
    }

    @Override
    public String description() {
        return "Read F3 debug screen text plus exact JVM, garbage collector, memory-pool, and OS data";
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
        return ClientThread.supply(DebugInfoCollector::capture)
                .thenApply(text -> new ToolResult(invocation.callId(), text, false, List.of()))
                .exceptionally(failure ->
                        new ToolResult(invocation.callId(), "Could not read debug information.", true, List.of()));
    }
}
