package com.izakji.agent.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
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
}
