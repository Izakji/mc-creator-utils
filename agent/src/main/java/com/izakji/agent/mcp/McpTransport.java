package com.izakji.agent.mcp;

import com.google.gson.JsonObject;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * JSON-RPC transport for one MCP server.
 *
 * @author Izakji
 */
public interface McpTransport {
    CompletableFuture<Void> start(Consumer<JsonObject> listener);

    void send(JsonObject message);

    void close();
}
