package com.izakji.agent.mcp;

import com.google.gson.JsonObject;
import com.izakji.agent.api.AgentTool;
import com.izakji.agent.api.ToolContext;
import com.izakji.agent.api.ToolInvocation;
import com.izakji.agent.api.ToolResult;
import com.izakji.agent.api.ToolRisk;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Reads one MCP server resource.
 *
 * @author Izakji
 */
public final class McpResourceTool implements AgentTool {
    private final McpClient client;
    private final String serverId;

    public McpResourceTool(McpClient client, String serverId) {
        this.client = client;
        this.serverId = serverId;
    }

    @Override
    public String name() {
        return "mcp__" + serverId + "__read_resource";
    }

    @Override
    public String description() {
        return "Read an MCP resource by URI";
    }

    @Override
    public JsonObject inputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject properties = new JsonObject();
        JsonObject uri = new JsonObject();
        uri.addProperty("type", "string");
        properties.add("uri", uri);
        schema.add("properties", properties);
        schema.add("required", stringArray("uri"));
        return schema;
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.READ_ONLY;
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolInvocation invocation, ToolContext context) {
        String uri = invocation.arguments().has("uri")
                ? invocation.arguments().get("uri").getAsString()
                : "";
        return client.readResource(uri)
                .thenApply(resource ->
                        new ToolResult(invocation.callId(), resource.text(), false, List.of()));
    }

    private static com.google.gson.JsonArray stringArray(String value) {
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        array.add(value);
        return array;
    }
}
