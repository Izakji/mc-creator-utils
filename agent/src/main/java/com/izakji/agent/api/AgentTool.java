package com.izakji.agent.api;

import com.google.gson.JsonObject;
import java.util.concurrent.CompletableFuture;

/**
 * Tool callable by the agent.
 *
 * @author Izakji
 */
public interface AgentTool {
    String name();

    String description();

    JsonObject inputSchema();

    ToolRisk risk();

    default boolean allowsConversationApproval() {
        return true;
    }

    CompletableFuture<ToolResult> execute(ToolInvocation invocation, ToolContext context);
}
