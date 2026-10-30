package com.izakji.agent.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * OpenAI-compatible chat-completions adapter.
 *
 * @author Izakji
 */
public final class OpenAiCompatibleProvider implements LlmProvider {
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Override
    public ProviderType type() {
        return ProviderType.OPENAI_COMPATIBLE;
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
        return client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofLines())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new IllegalStateException("Provider returned HTTP " + response.statusCode());
                    }
                    consume(response.body(), listener, cancellationToken);
                    return null;
                });
    }

    private HttpRequest buildRequest(ChatRequest request) {
        ProviderProfile profile = request.profile();
        JsonObject body = new JsonObject();
        body.addProperty("model", profile.model());
        body.addProperty("stream", true);
        body.addProperty("temperature", profile.temperature());
        body.addProperty("max_tokens", request.maxOutputTokens());
        JsonArray messages = new JsonArray();
        request.messages().forEach(message -> messages.add(messageBody(message)));
        body.add("messages", messages);
        if (!request.tools().isEmpty()) {
            JsonArray tools = new JsonArray();
            request.tools().forEach(tool -> tools.add(toolBody(tool)));
            body.add("tools", tools);
            body.addProperty("tool_choice", "auto");
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(stripTrailingSlash(profile.baseUrl()) + "/chat/completions"))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
        for (Map.Entry<String, String> header : profile.headers().entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }
        if (!profile.apiKey().isBlank() && !profile.headers().containsKey("Authorization")) {
            builder.header("Authorization", "Bearer " + profile.apiKey());
        }
        return builder.build();
    }

    private JsonObject messageBody(ChatMessage message) {
        JsonObject object = new JsonObject();
        switch (message) {
            case SystemMessage system -> {
                object.addProperty("role", "system");
                object.addProperty("content", system.text());
            }
            case UserMessage user -> {
                object.addProperty("role", "user");
                object.add("content", content(user.parts()));
            }
            case AssistantMessage assistant -> {
                object.addProperty("role", "assistant");
                String text = text(assistant.parts());
                if (!text.isEmpty()) {
                    object.addProperty("content", text);
                }
                if (!assistant.toolCalls().isEmpty()) {
                    JsonArray calls = new JsonArray();
                    assistant.toolCalls().forEach(call -> {
                        JsonObject function = new JsonObject();
                        function.addProperty("name", call.name());
                        function.addProperty("arguments", call.arguments().toString());
                        JsonObject value = new JsonObject();
                        value.addProperty("id", call.id());
                        value.addProperty("type", "function");
                        value.add("function", function);
                        calls.add(value);
                    });
                    object.add("tool_calls", calls);
                }
            }
            case ToolMessage tool -> {
                object.addProperty("role", "tool");
                object.addProperty("tool_call_id", tool.toolCallId());
                object.addProperty("content", text(tool.parts()));
            }
        }
        return object;
    }

    private JsonObject toolBody(ToolSchema tool) {
        JsonObject function = new JsonObject();
        function.addProperty("name", tool.name());
        function.addProperty("description", tool.description());
        function.add("parameters", tool.inputSchema());
        JsonObject object = new JsonObject();
        object.addProperty("type", "function");
        object.add("function", function);
        return object;
    }

    private JsonElement content(List<ContentPart> parts) {
        boolean image = parts.stream().anyMatch(ImagePart.class::isInstance);
        if (!image) {
            return new com.google.gson.JsonPrimitive(text(parts));
        }
        JsonArray array = new JsonArray();
        for (ContentPart part : parts) {
            if (part instanceof TextPart text) {
                JsonObject item = new JsonObject();
                item.addProperty("type", "text");
                item.addProperty("text", text.text());
                array.add(item);
            } else if (part instanceof ImagePart imagePart) {
                JsonObject item = new JsonObject();
                item.addProperty("type", "image_url");
                JsonObject url = new JsonObject();
                url.addProperty(
                        "url",
                        "data:" + imagePart.mediaType() + ";base64,"
                                + Base64.getEncoder().encodeToString(imagePart.data()));
                item.add("image_url", url);
                array.add(item);
            }
        }
        return array;
    }

    private static String text(List<ContentPart> parts) {
        List<String> values = new ArrayList<>();
        for (ContentPart part : parts) {
            if (part instanceof TextPart text) {
                values.add(text.text());
            } else if (part instanceof ReasoningPart reasoning && reasoning.signature().isEmpty()) {
                values.add(reasoning.text());
            }
        }
        return String.join("\n", values);
    }

    private void consume(Stream<String> lines, StreamListener listener, CancellationToken cancellationToken) {
        SseParser parser = new SseParser();
        Map<Integer, ToolBuilder> tools = new TreeMap<>();
        FinishReason finish = FinishReason.STOP;
        try (lines) {
            for (String line : lines.toList()) {
                cancellationToken.throwIfCancelled();
                for (SseEvent event : parser.accept(line + "\n")) {
                    JsonObject chunk = JsonParser.parseString(event.data()).getAsJsonObject();
                    if (chunk.has("usage")) {
                        JsonObject usage = chunk.getAsJsonObject("usage");
                        listener.onUsage(
                                intValue(usage, "prompt_tokens"),
                                intValue(usage, "completion_tokens"));
                    }
                    if (!chunk.has("choices") || chunk.getAsJsonArray("choices").isEmpty()) {
                        continue;
                    }
                    JsonObject choice = chunk.getAsJsonArray("choices").get(0).getAsJsonObject();
                    if (choice.has("finish_reason") && !choice.get("finish_reason").isJsonNull()) {
                        String reason = choice.get("finish_reason").getAsString();
                        if ("tool_calls".equals(reason)) {
                            finish = FinishReason.TOOL_CALLS;
                        } else if ("length".equals(reason)) {
                            finish = FinishReason.LENGTH;
                        }
                    }
                    if (!choice.has("delta")) {
                        continue;
                    }
                    JsonObject delta = choice.getAsJsonObject("delta");
                    if (delta.has("content") && !delta.get("content").isJsonNull()) {
                        listener.onTextDelta(delta.get("content").getAsString());
                    }
                    if (delta.has("reasoning_content") && !delta.get("reasoning_content").isJsonNull()) {
                        listener.onReasoningDelta(delta.get("reasoning_content").getAsString());
                    }
                    if (delta.has("tool_calls")) {
                        for (JsonElement element : delta.getAsJsonArray("tool_calls")) {
                            JsonObject call = element.getAsJsonObject();
                            int index = call.has("index") ? call.get("index").getAsInt() : 0;
                            ToolBuilder builder = tools.computeIfAbsent(index, ignored -> new ToolBuilder());
                            if (call.has("id")) {
                                builder.id = call.get("id").getAsString();
                            }
                            if (call.has("function")) {
                                JsonObject function = call.getAsJsonObject("function");
                                if (function.has("name")) {
                                    builder.name = function.get("name").getAsString();
                                }
                                if (function.has("arguments")) {
                                    builder.arguments.append(function.get("arguments").getAsString());
                                }
                            }
                        }
                    }
                }
            }
        }
        for (ToolBuilder builder : tools.values()) {
            listener.onToolCall(builder.build());
        }
        if (!tools.isEmpty()) {
            finish = FinishReason.TOOL_CALLS;
        }
        listener.onFinish(finish);
    }

    private static int intValue(JsonObject object, String name) {
        return object.has(name) ? object.get(name).getAsInt() : 0;
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static final class ToolBuilder {
        private String id = "";
        private String name = "";
        private final StringBuilder arguments = new StringBuilder();

        private ToolCall build() {
            String json = arguments.isEmpty() ? "{}" : arguments.toString();
            JsonObject parsed;
            try {
                parsed = JsonParser.parseString(json).getAsJsonObject();
            } catch (RuntimeException malformed) {
                parsed = new JsonObject();
            }
            return new ToolCall(id.isEmpty() ? "tool-call" : id, name, parsed);
        }
    }
}
