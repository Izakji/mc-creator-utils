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
 * Read-only CurseForge file detail lookup.
 *
 * @author Izakji
 */
public final class CurseForgeFileTool implements AgentTool {
    private final CurseForgeClient client;

    public CurseForgeFileTool(CurseForgeClient client) {
        this.client = client;
    }

    @Override
    public String name() {
        return "curseforge_file";
    }

    @Override
    public String description() {
        return "Read one CurseForge file by mod id and file id, including hashes, dependencies, "
                + "download URL, and optional changelog.";
    }

    @Override
    public JsonObject inputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject properties = new JsonObject();
        JsonObject modId = new JsonObject();
        modId.addProperty("type", "integer");
        properties.add("modId", modId);
        JsonObject fileId = new JsonObject();
        fileId.addProperty("type", "integer");
        properties.add("fileId", fileId);
        JsonObject changelog = new JsonObject();
        changelog.addProperty("type", "boolean");
        properties.add("includeChangelog", changelog);
        schema.add("properties", properties);
        JsonArray required = new JsonArray();
        required.add("modId");
        required.add("fileId");
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
        long fileId = ToolArguments.integer(invocation.arguments(), "fileId", 0);
        if (modId <= 0 || fileId <= 0) {
            return CompletableFuture.completedFuture(new ToolResult(
                    invocation.callId(), "modId and fileId must be positive CurseForge ids.", true, List.of()));
        }
        boolean includeChangelog = ToolArguments.bool(invocation.arguments(), "includeChangelog", false);
        return client.file(modId, fileId, includeChangelog)
                .thenApply(
                        text -> new ToolResult(invocation.callId(), text, CurseForgeClient.isError(text), List.of()));
    }
}
