package com.izakji.agent.mcp;

import com.google.gson.JsonObject;
import com.izakji.agent.api.AgentTool;
import com.izakji.agent.api.ContentPart;
import com.izakji.agent.api.TextPart;
import com.izakji.agent.api.ToolContext;
import com.izakji.agent.api.ToolInvocation;
import com.izakji.agent.api.ToolResult;
import com.izakji.agent.api.ToolRisk;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Loads one MCP server prompt.
 *
 * @author Izakji
 */
public final class McpPromptTool implements AgentTool {
    private final McpClient client;
    private final String serverId;

    public McpPromptTool(McpClient client, String serverId) {
        this.client = client;
        this.serverId = serverId;
    }

    @Override
    public String name() {
        return "mcp__" + serverId + "__load_prompt";
    }

    @Override
    public String description() {
        return "Load an MCP prompt by name";
    }

    @Override
    public JsonObject inputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject properties = new JsonObject();
        JsonObject name = new JsonObject();
        name.addProperty("type", "string");
        properties.add("name", name);
        JsonObject arguments = new JsonObject();
        arguments.addProperty("type", "object");
        properties.add("arguments", arguments);
        schema.add("properties", properties);
        com.google.gson.JsonArray required = new com.google.gson.JsonArray();
        required.add("name");
        schema.add("required", required);
        return schema;
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.READ_ONLY;
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolInvocation invocation, ToolContext context) {
        String name = invocation.arguments().has("name")
                ? invocation.arguments().get("name").getAsString()
                : "";
        JsonObject arguments = invocation.arguments().has("arguments")
                ? invocation.arguments().getAsJsonObject("arguments")
                : new JsonObject();
        return client.getPrompt(name, arguments).thenApply(result -> {
            StringBuilder text = new StringBuilder();
            for (var message : result.messages()) {
                for (ContentPart part : parts(message)) {
                    if (part instanceof TextPart value) {
                        if (!text.isEmpty()) {
                            text.append('\n');
                        }
                        text.append(value.text());
                    }
                }
            }
            return new ToolResult(invocation.callId(), text.toString(), false, List.of());
        });
    }

    private static List<ContentPart> parts(com.izakji.agent.api.ChatMessage message) {
        if (message instanceof com.izakji.agent.api.UserMessage user) {
            return user.parts();
        }
        if (message instanceof com.izakji.agent.api.AssistantMessage assistant) {
            return assistant.parts();
        }
        return List.of();
    }
}
