package com.izakji.agent.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.izakji.agent.api.AssistantMessage;
import com.izakji.agent.api.CancellationToken;
import com.izakji.agent.api.ChatMessage;
import com.izakji.agent.api.ChatRequest;
import com.izakji.agent.api.ContentPart;
import com.izakji.agent.api.FinishReason;
import com.izakji.agent.api.ImagePart;
import com.izakji.agent.api.LlmProvider;
import com.izakji.agent.api.ReasoningPart;
import com.izakji.agent.api.StreamListener;
import com.izakji.agent.api.SystemMessage;
import com.izakji.agent.api.TextPart;
import com.izakji.agent.api.ToolCall;
import com.izakji.agent.api.ToolMessage;
import com.izakji.agent.api.ToolSchema;
import com.izakji.agent.api.UserMessage;
import com.izakji.agent.config.ProviderProfile;
import com.izakji.agent.config.ProviderType;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Anthropic Messages API adapter.
 *
 * @author Izakji
 */
public final class AnthropicProvider implements LlmProvider {
    private static final String DEFAULT_VERSION = "2023-06-01";
    private final HttpClient client =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    @Override
    public ProviderType type() {
        return ProviderType.ANTHROPIC;
    }

    @Override
    public boolean supportsTools() {
        return true;
    }

    @Override
    public CompletableFuture<Void> stream(
            ChatRequest request, StreamListener listener, CancellationToken cancellationToken) {
        HttpRequest httpRequest;
        try {
            httpRequest = buildRequest(request);
        } catch (RuntimeException failure) {
            return CompletableFuture.failedFuture(failure);
        }
        return ProviderStreamingHttp.execute(
                client,
                httpRequest,
                cancellationToken,
                lines -> consume(lines, listener, cancellationToken),
                scrubber(request.profile()));
    }

    private static java.util.function.UnaryOperator<String> scrubber(ProviderProfile profile) {
        java.util.Set<String> secrets = new java.util.LinkedHashSet<>();
        if (!profile.apiKey().isBlank()) {
            secrets.add(profile.apiKey());
        }
        secrets.addAll(profile.headers().values());
        com.izakji.agent.util.Redactor redactor = new com.izakji.agent.util.Redactor(secrets);
        return redactor::redact;
    }

    private HttpRequest buildRequest(ChatRequest request) {
        ProviderProfile profile = request.profile();
        JsonObject body = new JsonObject();
        body.addProperty("model", profile.model());
        body.addProperty("max_tokens", request.maxOutputTokens());
        body.addProperty("temperature", profile.temperature());
        body.addProperty("stream", true);
        if (profile.thinkingBudgetTokens() > 0) {
            JsonObject thinking = new JsonObject();
            thinking.addProperty("type", "enabled");
            thinking.addProperty(
                    "budget_tokens",
                    Math.min(profile.thinkingBudgetTokens(), Math.max(1, request.maxOutputTokens() - 1)));
            body.add("thinking", thinking);
        }

        List<String> system = new ArrayList<>();
        JsonArray messages = new JsonArray();
        for (ChatMessage message : request.messages()) {
            switch (message) {
                case SystemMessage systemMessage -> system.add(systemMessage.text());
                case UserMessage user -> messages.add(message("user", blocks(user.parts())));
                case AssistantMessage assistant -> {
                    JsonArray blocks = blocks(assistant.parts());
                    for (ToolCall call : assistant.toolCalls()) {
                        JsonObject block = new JsonObject();
                        block.addProperty("type", "tool_use");
                        block.addProperty("id", call.id());
                        block.addProperty("name", call.name());
                        block.add("input", call.arguments());
                        blocks.add(block);
                    }
                    messages.add(message("assistant", blocks));
                }
                case ToolMessage tool -> {
                    JsonObject block = new JsonObject();
                    block.addProperty("type", "tool_result");
                    block.addProperty("tool_use_id", tool.toolCallId());
                    block.addProperty("content", text(tool.parts()));
                    block.addProperty("is_error", tool.error());
                    JsonArray blocks = new JsonArray();
                    blocks.add(block);
                    messages.add(message("user", blocks));
                }
            }
        }
        if (!system.isEmpty()) {
            body.addProperty("system", String.join("\n", system));
        }
        body.add("messages", messages);

        if (!request.tools().isEmpty()) {
            JsonArray tools = new JsonArray();
            request.tools().forEach(tool -> tools.add(tool(tool)));
            body.add("tools", tools);
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(stripTrailingSlash(profile.baseUrl()) + "/v1/messages"))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
        for (Map.Entry<String, String> header : profile.headers().entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }
        if (!profile.apiKey().isBlank() && !profile.headers().containsKey("x-api-key")) {
            builder.header("x-api-key", profile.apiKey());
        }
        if (!profile.headers().containsKey("anthropic-version")) {
            builder.header("anthropic-version", DEFAULT_VERSION);
        }
        return builder.build();
    }

    private static JsonObject message(String role, JsonArray content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.add("content", content);
        return message;
    }

    private JsonObject tool(ToolSchema tool) {
        JsonObject object = new JsonObject();
        object.addProperty("name", tool.name());
        object.addProperty("description", tool.description());
        object.add("input_schema", tool.inputSchema());
        return object;
    }

    private JsonArray blocks(List<ContentPart> parts) {
        JsonArray blocks = new JsonArray();
        for (ContentPart part : parts) {
            if (part instanceof TextPart text) {
                JsonObject block = new JsonObject();
                block.addProperty("type", "text");
                block.addProperty("text", text.text());
                blocks.add(block);
            } else if (part instanceof ReasoningPart reasoning) {
                if (reasoning.signature().startsWith("redacted:")) {
                    JsonObject block = new JsonObject();
                    block.addProperty("type", "redacted_thinking");
                    block.addProperty("data", reasoning.signature().substring("redacted:".length()));
                    blocks.add(block);
                } else if (!reasoning.signature().isEmpty()) {
                    JsonObject block = new JsonObject();
                    block.addProperty("type", "thinking");
                    block.addProperty("thinking", reasoning.text());
                    block.addProperty("signature", reasoning.signature());
                    blocks.add(block);
                }
            } else if (part instanceof ImagePart image) {
                JsonObject source = new JsonObject();
                source.addProperty("type", "base64");
                source.addProperty("media_type", image.mediaType());
                source.addProperty("data", Base64.getEncoder().encodeToString(image.data()));
                JsonObject block = new JsonObject();
                block.addProperty("type", "image");
                block.add("source", source);
                blocks.add(block);
            }
        }
        return blocks;
    }

    private void consume(Stream<String> lines, StreamListener listener, CancellationToken cancellationToken) {
        SseParser parser = new SseParser();
        StringBuilder reasoning = new StringBuilder();
        StringBuilder signature = new StringBuilder();
        StringBuilder toolInput = new StringBuilder();
        String toolId = "";
        String toolName = "";
        String blockType = "";
        boolean sawTool = false;
        FinishReason finish = FinishReason.STOP;
        try (lines) {
            java.util.Iterator<String> iterator = lines.iterator();
            while (iterator.hasNext()) {
                String line = iterator.next();
                cancellationToken.throwIfCancelled();
                for (SseEvent event : parser.accept(line + "\n")) {
                    JsonObject data = event.data().isBlank()
                            ? new JsonObject()
                            : JsonParser.parseString(event.data()).getAsJsonObject();
                    switch (event.event()) {
                        case "content_block_start" -> {
                            JsonObject block = data.getAsJsonObject("content_block");
                            blockType = block.get("type").getAsString();
                            reasoning.setLength(0);
                            signature.setLength(0);
                            toolInput.setLength(0);
                            if ("thinking".equals(blockType)) {
                                if (block.has("thinking")) {
                                    reasoning.append(block.get("thinking").getAsString());
                                }
                                if (block.has("signature")) {
                                    signature.append(block.get("signature").getAsString());
                                }
                            } else if ("redacted_thinking".equals(blockType)) {
                                if (block.has("data")) {
                                    signature
                                            .append("redacted:")
                                            .append(block.get("data").getAsString());
                                }
                            } else if ("tool_use".equals(blockType)) {
                                toolId = block.get("id").getAsString();
                                toolName = block.get("name").getAsString();
                            }
                        }
                        case "content_block_delta" -> {
                            JsonObject delta = data.getAsJsonObject("delta");
                            String type = delta.get("type").getAsString();
                            if ("thinking_delta".equals(type)) {
                                String text = delta.get("thinking").getAsString();
                                reasoning.append(text);
                                listener.onReasoningDelta(text);
                            } else if ("text_delta".equals(type)) {
                                listener.onTextDelta(delta.get("text").getAsString());
                            } else if ("signature_delta".equals(type)) {
                                signature.append(delta.get("signature").getAsString());
                            } else if ("input_json_delta".equals(type)) {
                                toolInput.append(delta.get("partial_json").getAsString());
                            }
                        }
                        case "content_block_stop" -> {
                            if ("thinking".equals(blockType) && !signature.isEmpty()) {
                                listener.onReasoningSignature(signature.toString());
                            } else if ("redacted_thinking".equals(blockType) && !signature.isEmpty()) {
                                listener.onReasoningSignature(signature.toString());
                            } else if ("tool_use".equals(blockType)) {
                                listener.onToolCall(new ToolCall(toolId, toolName, parse(toolInput.toString())));
                                sawTool = true;
                            }
                            blockType = "";
                        }
                        case "message_delta" -> {
                            if (data.has("usage")) {
                                JsonObject usage = data.getAsJsonObject("usage");
                                listener.onUsage(intValue(usage, "input_tokens"), intValue(usage, "output_tokens"));
                            }
                            if (data.has("delta")
                                    && data.getAsJsonObject("delta").has("stop_reason")) {
                                String reason = data.getAsJsonObject("delta")
                                        .get("stop_reason")
                                        .getAsString();
                                if ("tool_use".equals(reason)) {
                                    finish = FinishReason.TOOL_CALLS;
                                } else if ("max_tokens".equals(reason)) {
                                    finish = FinishReason.LENGTH;
                                }
                            }
                        }
                        case "message_stop" -> {}
                        case "error" -> throw new IllegalStateException("Provider error: " + data);
                        default -> {}
                    }
                }
            }
        }
        if (sawTool) {
            finish = FinishReason.TOOL_CALLS;
        }
        listener.onFinish(finish);
    }

    private static JsonObject parse(String json) {
        if (json == null || json.isBlank()) {
            return new JsonObject();
        }
        try {
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException malformed) {
            return new JsonObject();
        }
    }

    private static int intValue(JsonObject object, String name) {
        return object.has(name) ? object.get(name).getAsInt() : 0;
    }

    private static String text(List<ContentPart> parts) {
        List<String> values = new ArrayList<>();
        for (ContentPart part : parts) {
            if (part instanceof TextPart text) {
                values.add(text.text());
            }
        }
        return String.join("\n", values);
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
