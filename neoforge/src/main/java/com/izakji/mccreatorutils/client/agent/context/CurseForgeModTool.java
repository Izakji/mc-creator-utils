package com.izakji.mccreatorutils.client.agent.context;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.izakji.agent.api.AgentTool;
import com.izakji.agent.api.ToolContext;
import com.izakji.agent.api.ToolInvocation;
import com.izakji.agent.api.ToolResult;
import com.izakji.agent.api.ToolRisk;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Read-only CurseForge mod detail lookup.
 *
 * @author Izakji
 */
public final class CurseForgeModTool implements AgentTool {
    private final CurseForgeClient client;

    public CurseForgeModTool(CurseForgeClient client) {
        this.client = client;
    }

    @Override
    public String name() {
        return "curseforge_mod";
    }

    @Override
    public String description() {
        return "Read one CurseForge mod by numeric mod id, including authors, links, categories, "
                + "latest files, and optional description.";
    }

    @Override
    public JsonObject inputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject properties = new JsonObject();
        JsonObject modId = new JsonObject();
        modId.addProperty("type", "integer");
        properties.add("modId", modId);
        JsonObject description = new JsonObject();
        description.addProperty("type", "boolean");
        properties.add("includeDescription", description);
        schema.add("properties", properties);
        JsonArray required = new JsonArray();
        required.add("modId");
        schema.add("required", required);
        return schema;
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.READ_ONLY;
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolInvocation invocation, ToolContext context) {
        long modId = ToolArguments.integer(invocation.arguments(), "modId", 0);
        if (modId <= 0) {
            return CompletableFuture.completedFuture(new ToolResult(
                    invocation.callId(), "modId must be a positive CurseForge mod id.", true, List.of()));
        }
        boolean includeDescription = ToolArguments.bool(invocation.arguments(), "includeDescription", false);
        return client.mod(modId, includeDescription)
                .thenApply(
                        text -> new ToolResult(invocation.callId(), text, CurseForgeClient.isError(text), List.of()));
    }
}
