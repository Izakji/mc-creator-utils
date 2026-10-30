package com.izakji.agent.mcp;

import com.google.gson.JsonObject;
import com.izakji.agent.api.AgentTool;
import com.izakji.agent.api.ContentPart;
import com.izakji.agent.api.ImagePart;
import com.izakji.agent.api.TextPart;
import com.izakji.agent.api.ToolContext;
import com.izakji.agent.api.ToolInvocation;
import com.izakji.agent.api.ToolResult;
import com.izakji.agent.api.ToolRisk;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Agent tool backed by one MCP server tool.
 *
 * @author Izakji
 */
public final class McpTool implements AgentTool {
    private final McpClient client;
    private final String serverId;
    private final McpToolDescriptor descriptor;

    public McpTool(McpClient client, String serverId, McpToolDescriptor descriptor) {
        this.client = client;
        this.serverId = serverId;
        this.descriptor = descriptor;
    }

    @Override
    public String name() {
        return "mcp__" + serverId + "__" + descriptor.name();
    }

    @Override
    public String description() {
        return descriptor.description();
    }

    @Override
    public JsonObject inputSchema() {
        return descriptor.inputSchema();
    }

    @Override
    public ToolRisk risk() {
        if (descriptor.readOnly()) {
            return ToolRisk.READ_ONLY;
        }
        return descriptor.destructive() ? ToolRisk.DANGEROUS : ToolRisk.MUTATING;
    }

    @Override
    public boolean allowsConversationApproval() {
        return risk() == ToolRisk.READ_ONLY;
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolInvocation invocation, ToolContext context) {
        return client.callTool(descriptor.name(), invocation.arguments()).thenApply(result -> {
            StringBuilder text = new StringBuilder();
            List<ContentPart> attachments = new ArrayList<>();
            for (ContentPart part : result.parts()) {
                if (part instanceof TextPart value) {
                    if (!text.isEmpty()) {
                        text.append('\n');
                    }
                    text.append(value.text());
                } else if (part instanceof ImagePart) {
                    attachments.add(part);
                }
            }
            return new ToolResult(invocation.callId(), text.toString(), result.error(), attachments);
        });
    }
}
