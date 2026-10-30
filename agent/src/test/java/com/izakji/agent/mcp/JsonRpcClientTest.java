package com.izakji.agent.mcp;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonObject;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class JsonRpcClientTest {

    @Test
    void matchesResponsesByRequestId() {
        FakeTransport transport = new FakeTransport();
        JsonRpcClient client = new JsonRpcClient(transport, Duration.ofMillis(500));
        CompletableFuture<JsonObject> future = client.request("tools/list", new JsonObject());
        transport.receive("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"tools\":[]}}");
        assertEquals(0, future.join().getAsJsonArray("tools").size());
    }

    @Test
    void matchesStringIdsAndIgnoresMethodMessages() {
        FakeTransport transport = new FakeTransport();
        JsonRpcClient client = new JsonRpcClient(transport, Duration.ofMillis(500));
        CompletableFuture<JsonObject> future = client.request("tools/list", new JsonObject());
        assertDoesNotThrow(() ->
                transport.receive("{\"jsonrpc\":\"2.0\",\"id\":\"server-1\",\"method\":\"sampling/createMessage\"}"));
        transport.receive("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"tools\":[]}}");
        assertEquals(0, future.join().getAsJsonArray("tools").size());
    }

    @Test
    void failsPendingRequestsWhenTheTransportCannotStart() {
        McpTransport transport = new McpTransport() {
            @Override
            public CompletableFuture<Void> start(Consumer<JsonObject> listener) {
                return CompletableFuture.failedFuture(new IllegalStateException("no process"));
            }

            @Override
            public void send(JsonObject message) {}

            @Override
            public void close() {}
        };
        JsonRpcClient client = new JsonRpcClient(transport, Duration.ofSeconds(1));
        ExecutionException failure = assertThrows(
                ExecutionException.class,
                () -> client.request("initialize", new JsonObject()).get());
        assertEquals("no process", failure.getCause().getMessage());
    }
}
