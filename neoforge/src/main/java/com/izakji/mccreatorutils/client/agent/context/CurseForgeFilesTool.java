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
 * Read-only CurseForge file listing for one mod.
 *
 * @author Izakji
 */
public final class CurseForgeFilesTool implements AgentTool {
    private final CurseForgeClient client;

    public CurseForgeFilesTool(CurseForgeClient client) {
        this.client = client;
    }

    @Override
    public String name() {
        return "curseforge_files";
    }

    @Override
    public String description() {
        return "List CurseForge files for a mod id, with optional gameVersion and mod loader filters. "
                + "Returns file ids, names, release types, versions, dependencies, and download URLs.";
    }

    @Override
    public JsonObject inputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject properties = new JsonObject();
        JsonObject modId = new JsonObject();
        modId.addProperty("type", "integer");
        properties.add("modId", modId);
        JsonObject gameVersion = new JsonObject();
        gameVersion.addProperty("type", "string");
        properties.add("gameVersion", gameVersion);
        JsonObject modLoader = new JsonObject();
        modLoader.addProperty("type", "string");
        JsonArray loaders = new JsonArray();
        loaders.add("neoforge");
        loaders.add("forge");
        loaders.add("fabric");
        loaders.add("quilt");
        loaders.add("any");
        modLoader.add("enum", loaders);
        properties.add("modLoader", modLoader);
        JsonObject limit = new JsonObject();
        limit.addProperty("type", "integer");
        limit.addProperty("minimum", 1);
        limit.addProperty("maximum", 20);
        properties.add("limit", limit);
        JsonObject index = new JsonObject();
        index.addProperty("type", "integer");
        index.addProperty("minimum", 0);
        properties.add("index", index);
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
        String gameVersion = ToolArguments.string(invocation.arguments(), "gameVersion", "");
        int modLoader = Integer.parseInt(CurseForgeReport.normalizeModLoader(
                ToolArguments.string(invocation.arguments(), "modLoader", "neoforge")));
        int limit = ToolArguments.integer(invocation.arguments(), "limit", 10);
        int index = ToolArguments.integer(invocation.arguments(), "index", 0);
        return client.files(modId, gameVersion, modLoader, limit, index)
                .thenApply(
                        text -> new ToolResult(invocation.callId(), text, CurseForgeClient.isError(text), List.of()));
    }
}
