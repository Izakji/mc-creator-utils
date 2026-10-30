package com.izakji.agent.mcp;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.concurrent.CompletableFuture;

final class FakeMcpClient implements McpClient {
    private final List<McpToolDescriptor> tools;
    private final List<McpResourceDescriptor> resources;
    private final List<McpPromptDescriptor> prompts;

    FakeMcpClient(
            List<McpToolDescriptor> tools, List<McpResourceDescriptor> resources, List<McpPromptDescriptor> prompts) {
        this.tools = List.copyOf(tools);
        this.resources = List.copyOf(resources);
        this.prompts = List.copyOf(prompts);
    }

    @Override
    public CompletableFuture<McpServerInfo> initialize() {
        return CompletableFuture.completedFuture(
                new McpServerInfo("fake", "1", "2025-06-18", java.util.Set.of("tools")));
    }

    @Override
    public CompletableFuture<List<McpToolDescriptor>> listTools() {
        return CompletableFuture.completedFuture(tools);
    }

    @Override
    public CompletableFuture<McpCallResult> callTool(String name, JsonObject arguments) {
        return CompletableFuture.completedFuture(
                new McpCallResult(List.of(new com.izakji.agent.api.TextPart("result")), false));
    }

    @Override
    public CompletableFuture<List<McpResourceDescriptor>> listResources() {
        return CompletableFuture.completedFuture(resources);
    }

    @Override
    public CompletableFuture<McpResourceContent> readResource(String uri) {
        return CompletableFuture.completedFuture(new McpResourceContent(uri, "text/plain", "result"));
    }

    @Override
    public CompletableFuture<List<McpPromptDescriptor>> listPrompts() {
        return CompletableFuture.completedFuture(prompts);
    }

    @Override
    public CompletableFuture<McpPromptResult> getPrompt(String name, JsonObject arguments) {
        return CompletableFuture.completedFuture(new McpPromptResult("", List.of()));
    }

    @Override
    public void close() {}
}
