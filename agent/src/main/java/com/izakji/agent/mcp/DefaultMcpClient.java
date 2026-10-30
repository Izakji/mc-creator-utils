package com.izakji.agent.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.izakji.agent.api.AssistantMessage;
import com.izakji.agent.api.ChatMessage;
import com.izakji.agent.api.ContentPart;
import com.izakji.agent.api.ImagePart;
import com.izakji.agent.api.TextPart;
import com.izakji.agent.api.UserMessage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Default MCP client over JSON-RPC.
 *
 * @author Izakji
 */
public final class DefaultMcpClient implements McpClient {
    private static final String CLIENT_NAME = "creator-utils-agent";
    private final JsonRpcClient rpc;

    public DefaultMcpClient(McpTransport transport) {
        this.rpc = new JsonRpcClient(transport, Duration.ofSeconds(30));
    }

    @Override
    public CompletableFuture<McpServerInfo> initialize() {
        JsonObject params = new JsonObject();
        params.addProperty("protocolVersion", "2025-06-18");
        params.add("capabilities", new JsonObject());
        JsonObject clientInfo = new JsonObject();
        clientInfo.addProperty("name", CLIENT_NAME);
        clientInfo.addProperty("version", "1");
        params.add("clientInfo", clientInfo);
        return rpc.request("initialize", params).thenApply(result -> {
            JsonObject serverInfo = result.has("serverInfo")
                    ? result.getAsJsonObject("serverInfo")
                    : new JsonObject();
            Set<String> capabilities = new LinkedHashSet<>();
            if (result.has("capabilities")) {
                result.getAsJsonObject("capabilities").keySet().forEach(capabilities::add);
            }
            String name = serverInfo.has("name") ? serverInfo.get("name").getAsString() : "unknown";
            String version = serverInfo.has("version") ? serverInfo.get("version").getAsString() : "";
            String protocol = result.has("protocolVersion")
                    ? result.get("protocolVersion").getAsString()
                    : "2025-06-18";
            JsonObject notification = new JsonObject();
            rpc.notify("notifications/initialized", notification);
            return new McpServerInfo(name, version, protocol, capabilities);
        });
    }

    @Override
    public CompletableFuture<List<McpToolDescriptor>> listTools() {
        return rpc.request("tools/list", new JsonObject()).thenApply(result -> {
            List<McpToolDescriptor> tools = new ArrayList<>();
            if (result.has("tools")) {
                for (var element : result.getAsJsonArray("tools")) {
                    JsonObject tool = element.getAsJsonObject();
                    JsonObject annotations = tool.has("annotations")
                            ? tool.getAsJsonObject("annotations")
                            : new JsonObject();
                    tools.add(new McpToolDescriptor(
                            tool.get("name").getAsString(),
                            tool.has("description") ? tool.get("description").getAsString() : "",
                            tool.has("inputSchema") ? tool.getAsJsonObject("inputSchema") : new JsonObject(),
                            bool(annotations, "readOnlyHint"),
                            bool(annotations, "destructiveHint")));
                }
            }
            return List.copyOf(tools);
        });
    }

    @Override
    public CompletableFuture<McpCallResult> callTool(String name, JsonObject arguments) {
        JsonObject params = new JsonObject();
        params.addProperty("name", name);
        params.add("arguments", arguments == null ? new JsonObject() : arguments);
        return rpc.request("tools/call", params).thenApply(result -> {
            List<ContentPart> parts = new ArrayList<>();
            if (result.has("content")) {
                for (var element : result.getAsJsonArray("content")) {
                    JsonObject item = element.getAsJsonObject();
                    String type = item.has("type") ? item.get("type").getAsString() : "";
                    if ("text".equals(type)) {
                        parts.add(new TextPart(item.get("text").getAsString()));
                    } else if ("image".equals(type)) {
                        parts.add(new ImagePart(
                                item.has("mimeType") ? item.get("mimeType").getAsString() : "image/png",
                                Base64.getDecoder().decode(item.get("data").getAsString())));
                    }
                }
            }
            return new McpCallResult(parts, bool(result, "isError"));
        });
    }

    @Override
    public CompletableFuture<List<McpResourceDescriptor>> listResources() {
        return rpc.request("resources/list", new JsonObject()).thenApply(result -> {
            List<McpResourceDescriptor> resources = new ArrayList<>();
            if (result.has("resources")) {
                for (var element : result.getAsJsonArray("resources")) {
                    JsonObject resource = element.getAsJsonObject();
                    resources.add(new McpResourceDescriptor(
                            resource.get("uri").getAsString(),
                            resource.has("name") ? resource.get("name").getAsString() : "",
                            resource.has("description") ? resource.get("description").getAsString() : "",
                            resource.has("mimeType") ? resource.get("mimeType").getAsString() : ""));
                }
            }
            return List.copyOf(resources);
        });
    }

    @Override
    public CompletableFuture<McpResourceContent> readResource(String uri) {
        JsonObject params = new JsonObject();
        params.addProperty("uri", uri);
        return rpc.request("resources/read", params).thenApply(result -> {
            if (!result.has("contents") || result.getAsJsonArray("contents").isEmpty()) {
                return new McpResourceContent(uri, "text/plain", "");
            }
            JsonObject content = result.getAsJsonArray("contents").get(0).getAsJsonObject();
            return new McpResourceContent(
                    content.has("uri") ? content.get("uri").getAsString() : uri,
                    content.has("mimeType") ? content.get("mimeType").getAsString() : "text/plain",
                    content.has("text") ? content.get("text").getAsString() : "");
        });
    }

    @Override
    public CompletableFuture<List<McpPromptDescriptor>> listPrompts() {
        return rpc.request("prompts/list", new JsonObject()).thenApply(result -> {
            List<McpPromptDescriptor> prompts = new ArrayList<>();
            if (result.has("prompts")) {
                for (var element : result.getAsJsonArray("prompts")) {
                    JsonObject prompt = element.getAsJsonObject();
                    prompts.add(new McpPromptDescriptor(
                            prompt.get("name").getAsString(),
                            prompt.has("description") ? prompt.get("description").getAsString() : "",
                            prompt.has("arguments")
                                    ? prompt.getAsJsonObject("arguments")
                                    : new JsonObject()));
                }
            }
            return List.copyOf(prompts);
        });
    }

    @Override
    public CompletableFuture<McpPromptResult> getPrompt(String name, JsonObject arguments) {
        JsonObject params = new JsonObject();
        params.addProperty("name", name);
        params.add("arguments", arguments == null ? new JsonObject() : arguments);
        return rpc.request("prompts/get", params).thenApply(result -> {
            List<ChatMessage> messages = new ArrayList<>();
            if (result.has("messages")) {
                for (var element : result.getAsJsonArray("messages")) {
                    JsonObject message = element.getAsJsonObject();
                    String role = message.has("role") ? message.get("role").getAsString() : "user";
                    String text = message.has("content") && message.get("content").isJsonObject()
                            ? message.getAsJsonObject("content")
                                    .get("text")
                                    .getAsString()
                            : "";
                    messages.add("assistant".equals(role)
                            ? new AssistantMessage(List.of(new TextPart(text)), List.of())
                            : new UserMessage(List.of(new TextPart(text))));
                }
            }
            return new McpPromptResult(
                    result.has("description") ? result.get("description").getAsString() : "",
                    messages);
        });
    }

    @Override
    public void close() {
        rpc.close();
    }

    private static boolean bool(JsonObject object, String name) {
        return object.has(name) && object.get(name).getAsBoolean();
    }
}
