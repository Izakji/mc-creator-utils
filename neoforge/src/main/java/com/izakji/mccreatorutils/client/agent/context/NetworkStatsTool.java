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
 * Read-only ping and Minecraft connection traffic tool.
 *
 * @author Izakji
 */
public final class NetworkStatsTool implements AgentTool {

    @Override
    public String name() {
        return "network_stats";
    }

    @Override
    public String description() {
        return "Read ping, jitter, and Minecraft connection traffic rates";
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
        return CompletableFuture.completedFuture(new ToolResult(
                invocation.callId(),
                NetworkStatsReport.format(NetworkDiagnostics.instance().snapshot()),
                false,
                List.of()));
    }
}
