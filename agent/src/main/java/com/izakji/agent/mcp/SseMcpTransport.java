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
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Legacy SSE MCP transport.
 *
 * <p>The server opens a GET event stream and advertises a POST endpoint. Client messages are
 * posted to that endpoint and responses arrive on the event stream.</p>
 *
 * @author Izakji
 */
public final class SseMcpTransport implements McpTransport {
    private final McpServerConfig config;
    private final HttpClient client =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final CompletableFuture<Void> ready = new CompletableFuture<>();
    private volatile Consumer<JsonObject> listener = ignored -> {};
    private volatile String endpoint = "";
    private volatile CompletableFuture<HttpResponse<Stream<String>>> stream;

    public SseMcpTransport(McpServerConfig config) {
        this.config = config;
    }

    @Override
    public CompletableFuture<Void> start(Consumer<JsonObject> listener) {
        this.listener = listener;
        HttpRequest.Builder builder = request(URI.create(config.url()))
                .header("Accept", "text/event-stream")
                .GET();
        stream = client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofLines());
        stream.whenComplete((response, failure) -> {
            if (failure != null) {
                ready.completeExceptionally(failure);
                return;
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                ready.completeExceptionally(
                        new IllegalStateException("MCP SSE server returned HTTP " + response.statusCode()));
                return;
            }
            consume(response.body());
        });
        return ready.orTimeout(10, TimeUnit.SECONDS);
    }

    @Override
    public void send(JsonObject message) {
        String target = endpoint;
        if (target.isBlank()) {
            throw new IllegalStateException("MCP SSE endpoint is not ready");
        }
        HttpRequest.Builder builder = request(URI.create(target))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(message.toString(), StandardCharsets.UTF_8));
        client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString()).whenComplete((response, failure) -> {
            if (failure != null) {
                fail(message, "MCP request failed: " + failure.getMessage());
            } else if (response.statusCode() < 200 || response.statusCode() >= 300) {
                fail(message, "MCP server returned HTTP " + response.statusCode() + ": " + response.body());
            }
        });
    }

    @Override
    public void close() {
        CompletableFuture<HttpResponse<Stream<String>>> current = stream;
        if (current != null) {
            current.cancel(true);
        }
    }

    private HttpRequest.Builder request(URI uri) {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(uri).timeout(Duration.ofSeconds(120));
        for (Map.Entry<String, String> header : config.headers().entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }
        return builder;
    }

    private void consume(Stream<String> lines) {
        String event = "";
        StringBuilder data = new StringBuilder();
        try (lines) {
            for (String line : (Iterable<String>) lines::iterator) {
                if (line.isEmpty()) {
                    dispatch(event, data.toString());
                    event = "";
                    data.setLength(0);
                } else if (line.startsWith(":")) {
                    continue;
                } else if (line.startsWith("event:")) {
                    event = line.substring("event:".length()).stripLeading();
                } else if (line.startsWith("data:")) {
                    if (!data.isEmpty()) {
                        data.append('\n');
                    }
                    data.append(line.substring("data:".length()).stripLeading());
                }
            }
        } catch (RuntimeException ignored) {
            ready.completeExceptionally(new IllegalStateException("MCP SSE stream closed"));
        }
        if (!data.isEmpty()) {
            dispatch(event, data.toString());
        }
        ready.completeExceptionally(new IllegalStateException("MCP SSE stream closed"));
    }

    private void dispatch(String event, String data) {
        if (data.isBlank()) {
            return;
        }
        if ("endpoint".equals(event)) {
            endpoint = URI.create(config.url()).resolve(data.strip()).toString();
            ready.complete(null);
            return;
        }
        listener.accept(JsonParser.parseString(data).getAsJsonObject());
    }

    private void fail(JsonObject request, String message) {
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
}
