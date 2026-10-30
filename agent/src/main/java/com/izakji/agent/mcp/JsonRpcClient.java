package com.izakji.agent.mcp;

import com.google.gson.JsonObject;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Minimal JSON-RPC 2.0 client over an MCP transport.
 *
 * @author Izakji
 */
public final class JsonRpcClient implements AutoCloseable {
    private final McpTransport transport;
    private final Duration timeout;
    private final AtomicLong ids = new AtomicLong();
    private final Map<String, CompletableFuture<JsonObject>> pending = new ConcurrentHashMap<>();
    private final CompletableFuture<Void> started;

    public JsonRpcClient(McpTransport transport, Duration timeout) {
        this.transport = transport;
        this.timeout = timeout;
        this.started = transport.start(this::handle);
    }

    public CompletableFuture<JsonObject> request(String method, JsonObject params) {
        long id = ids.incrementAndGet();
        JsonObject message = new JsonObject();
        message.addProperty("jsonrpc", "2.0");
        message.addProperty("id", id);
        message.addProperty("method", method);
        message.add("params", params == null ? new JsonObject() : params);
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        CompletableFuture<JsonObject> timed = future.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
        return started.thenCompose(ignored -> {
            String key = Long.toString(id);
            pending.put(key, future);
            future.whenComplete((result, failure) -> pending.remove(key));
            try {
                transport.send(message);
            } catch (RuntimeException failure) {
                pending.remove(key);
                future.completeExceptionally(failure);
            }
            return timed;
        });
    }

    public void notify(String method, JsonObject params) {
        JsonObject message = new JsonObject();
        message.addProperty("jsonrpc", "2.0");
        message.addProperty("method", method);
        message.add("params", params == null ? new JsonObject() : params);
        started.thenRun(() -> transport.send(message));
    }

    public void close() {
        transport.close();
    }

    private void handle(JsonObject message) {
        if (message.has("method") || !message.has("id") || message.get("id").isJsonNull()) {
            return;
        }
        String key = message.get("id").getAsString();
        CompletableFuture<JsonObject> future = pending.remove(key);
        if (future == null) {
            return;
        }
        if (message.has("error")) {
            future.completeExceptionally(
                    new IllegalStateException(message.get("error").toString()));
        } else {
            future.complete(message.has("result") ? message.getAsJsonObject("result") : new JsonObject());
        }
    }
}
