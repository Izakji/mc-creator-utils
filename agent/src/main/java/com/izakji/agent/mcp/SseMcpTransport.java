package com.izakji.agent.mcp;

import com.google.gson.JsonObject;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Legacy SSE MCP transport.
 *
 * <p>The legacy protocol uses a GET stream for server messages and POST for client messages.
 * This implementation reuses the HTTP request path while keeping the transport type separate
 * for configuration and future endpoint negotiation.</p>
 *
 * @author Izakji
 */
public final class SseMcpTransport implements McpTransport {
    private final HttpMcpTransport delegate;

    public SseMcpTransport(McpServerConfig config) {
        this.delegate = new HttpMcpTransport(config);
    }

    @Override
    public CompletableFuture<Void> start(Consumer<JsonObject> listener) {
        return delegate.start(listener);
    }

    @Override
    public void send(JsonObject message) {
        delegate.send(message);
    }

    @Override
    public void close() {
        delegate.close();
    }
}
