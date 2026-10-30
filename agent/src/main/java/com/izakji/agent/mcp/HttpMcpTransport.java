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
        client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
            String session = response.headers().firstValue("Mcp-Session-Id").orElse("");
            if (!session.isBlank()) {
                sessionId = session;
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return;
            }
            dispatch(response.body());
        });
    }

    protected void dispatch(String body) {
        if (body == null || body.isBlank()) {
            return;
        }
        if (body.contains("data:") || body.contains("event:")) {
            SseParserLike.parse(body).forEach(event -> {
                if (!event.isBlank()) {
                    listener.accept(JsonParser.parseString(event).getAsJsonObject());
                }
            });
        } else {
            listener.accept(JsonParser.parseString(body).getAsJsonObject());
        }
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
