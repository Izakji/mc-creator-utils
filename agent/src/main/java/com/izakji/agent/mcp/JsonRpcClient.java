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
    private final Map<Long, CompletableFuture<JsonObject>> pending = new ConcurrentHashMap<>();

    public JsonRpcClient(McpTransport transport, Duration timeout) {
        this.transport = transport;
        this.timeout = timeout;
        transport.start(this::handle);
    }

    public CompletableFuture<JsonObject> request(String method, JsonObject params) {
        long id = ids.incrementAndGet();
        JsonObject message = new JsonObject();
        message.addProperty("jsonrpc", "2.0");
        message.addProperty("id", id);
        message.addProperty("method", method);
        message.add("params", params == null ? new JsonObject() : params);
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        pending.put(id, future);
        future.whenComplete((result, failure) -> pending.remove(id));
        try {
            transport.send(message);
        } catch (RuntimeException failure) {
            pending.remove(id);
            future.completeExceptionally(failure);
        }
        return future.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    public void notify(String method, JsonObject params) {
        JsonObject message = new JsonObject();
        message.addProperty("jsonrpc", "2.0");
        message.addProperty("method", method);
        message.add("params", params == null ? new JsonObject() : params);
        transport.send(message);
    }

    public void close() {
        transport.close();
    }

    private void handle(JsonObject message) {
        if (!message.has("id") || message.get("id").isJsonNull()) {
            return;
        }
        long id = message.get("id").getAsLong();
        CompletableFuture<JsonObject> future = pending.remove(id);
        if (future == null) {
            return;
        }
        if (message.has("error")) {
            future.completeExceptionally(new IllegalStateException(message.get("error").toString()));
        } else {
            future.complete(message.has("result")
                    ? message.getAsJsonObject("result")
                    : new JsonObject());
        }
    }
}
