package com.izakji.agent.mcp;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * MCP server client contract.
 *
 * @author Izakji
 */
public interface McpClient extends AutoCloseable {
    CompletableFuture<McpServerInfo> initialize();

    CompletableFuture<List<McpToolDescriptor>> listTools();

    CompletableFuture<McpCallResult> callTool(String name, JsonObject arguments);

    CompletableFuture<List<McpResourceDescriptor>> listResources();

    CompletableFuture<McpResourceContent> readResource(String uri);

    CompletableFuture<List<McpPromptDescriptor>> listPrompts();

    CompletableFuture<McpPromptResult> getPrompt(String name, JsonObject arguments);

    @Override
    void close();
}
