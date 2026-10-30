package com.izakji.agent.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class StdioMcpTransportTest {

    @Test
    void writesNewlineJsonAndDispatchesAStdoutResponse() throws Exception {
        FakeProcess process = FakeProcess.create();
        StdioMcpTransport transport = new StdioMcpTransport(McpTestSupport.config("server"), builder -> process);
        List<JsonObject> received = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        transport
                .start(json -> {
                    received.add(json);
                    latch.countDown();
                })
                .get(1, TimeUnit.SECONDS);

        JsonObject request = new JsonObject();
        request.addProperty("jsonrpc", "2.0");
        request.addProperty("id", 1);
        request.addProperty("method", "initialize");
        transport.send(request);
        assertEquals(request.toString(), process.stdinReader().readLine());

        process.writeStdout("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}\n");
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(1, received.get(0).get("id").getAsInt());
        transport.close();
    }
}
