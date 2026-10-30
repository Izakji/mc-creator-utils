package com.izakji.agent.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class HttpMcpTransportTest {

    @Test
    void postsJsonAndDispatchesAnSseResponse() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/mcp", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] body = "data: {\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}\n\n"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            McpServerConfig config = new McpServerConfig(
                    "server",
                    "Server",
                    McpTransportType.STREAMABLE_HTTP,
                    null,
                    List.of(),
                    null,
                    Map.of(),
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/mcp",
                    Map.of(),
                    true,
                    false);
            HttpMcpTransport transport = new HttpMcpTransport(config);
            List<JsonObject> received = new CopyOnWriteArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);
            transport.start(json -> {
                        received.add(json);
                        latch.countDown();
                    })
                    .get(1, TimeUnit.SECONDS);
            JsonObject request = new JsonObject();
            request.addProperty("jsonrpc", "2.0");
            request.addProperty("id", 1);
            request.addProperty("method", "initialize");
            transport.send(request);
            assertTrue(latch.await(1, TimeUnit.SECONDS));
            assertEquals(1, received.get(0).get("id").getAsInt());
        } finally {
            server.stop(0);
        }
    }
}
