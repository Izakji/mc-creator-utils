package com.izakji.agent.mcp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Streamable HTTP MCP transport.
 *
 * @author Izakji
 */
public final class HttpMcpTransport implements McpTransport {
    protected final McpServerConfig config;
    private final HttpClient client =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private volatile Consumer<JsonObject> listener = ignored -> {};
    private volatile String sessionId = "";

    public HttpMcpTransport(McpServerConfig config) {
        this.config = config;
    }

    @Override
    public CompletableFuture<Void> start(Consumer<JsonObject> listener) {
        this.listener = listener;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void send(JsonObject message) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(config.url()))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(message.toString(), StandardCharsets.UTF_8));
        for (Map.Entry<String, String> header : config.headers().entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }
        if (!sessionId.isBlank()) {
            builder.header("Mcp-Session-Id", sessionId);
        }
        client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString()).whenComplete((response, failure) -> {
            if (failure != null) {
                fail(message, "MCP request failed: " + failure.getMessage());
                return;
            }
            String session = response.headers().firstValue("Mcp-Session-Id").orElse("");
            if (!session.isBlank()) {
                sessionId = session;
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                fail(message, "MCP server returned HTTP " + response.statusCode() + ": " + response.body());
                return;
            }
            try {
                dispatch(
                        response.body(),
                        response.headers().firstValue("Content-Type").orElse(""));
            } catch (RuntimeException malformed) {
                fail(message, "Could not read MCP response: " + malformed.getMessage());
            }
        });
    }

    protected void dispatch(String body, String contentType) {
        if (body == null || body.isBlank()) {
            return;
        }
        String type = contentType == null ? "" : contentType.toLowerCase(java.util.Locale.ROOT);
        if (type.startsWith("text/event-stream")) {
            SseParserLike.parse(body).forEach(event -> {
                if (!event.isBlank()) {
                    listener.accept(JsonParser.parseString(event).getAsJsonObject());
                }
            });
        } else {
            listener.accept(JsonParser.parseString(body).getAsJsonObject());
        }
    }

    protected void fail(JsonObject request, String message) {
        if (!request.has("id") || request.get("id").isJsonNull()) {
            return;
        }
        JsonObject error = new JsonObject();
        error.addProperty("code", -32603);
        error.addProperty("message", message);
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.add("id", request.get("id").deepCopy());
        response.add("error", error);
        listener.accept(response);
    }

    @Override
    public void close() {}

    /** Small adapter kept local so transport code does not depend on provider SSE classes. */
    private static final class SseParserLike {
        static java.util.List<String> parse(String body) {
            java.util.List<String> events = new java.util.ArrayList<>();
            StringBuilder data = new StringBuilder();
            for (String line : body.replace("\r\n", "\n").split("\n", -1)) {
                if (line.isEmpty()) {
                    if (!data.isEmpty()) {
                        events.add(data.toString());
                        data.setLength(0);
                    }
                } else if (line.startsWith("data:")) {
                    if (!data.isEmpty()) {
                        data.append('\n');
                    }
                    data.append(line.substring(5).stripLeading());
                }
            }
            if (!data.isEmpty()) {
                events.add(data.toString());
            }
            return events;
        }
    }
}
