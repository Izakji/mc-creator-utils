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

class SseMcpTransportTest {

    @Test
    void opensTheEventStreamThenPostsToTheAdvertisedEndpoint() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        CountDownLatch requestSeen = new CountDownLatch(1);
        List<String> posted = new CopyOnWriteArrayList<>();
        server.createContext("/sse", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            byte[] endpoint =
                    ("event: endpoint\ndata: " + base + "/message?sessionId=abc\n\n").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().write(endpoint);
            exchange.getResponseBody().flush();
            try {
                requestSeen.await(2, TimeUnit.SECONDS);
                byte[] response = "event: message\ndata: {\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}\n\n"
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseBody().write(response);
                exchange.getResponseBody().flush();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });
        server.createContext("/message", exchange -> {
            posted.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(202, -1);
            exchange.close();
            requestSeen.countDown();
        });
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        try {
            McpServerConfig config = new McpServerConfig(
                    "legacy",
                    "Legacy",
                    McpTransportType.SSE,
                    null,
                    List.of(),
                    null,
                    Map.of(),
                    base + "/sse",
                    Map.of(),
                    true,
                    false);
            SseMcpTransport transport = new SseMcpTransport(config);
            List<JsonObject> received = new CopyOnWriteArrayList<>();
            CountDownLatch responseSeen = new CountDownLatch(1);
            transport
                    .start(json -> {
                        received.add(json);
                        responseSeen.countDown();
                    })
                    .get(2, TimeUnit.SECONDS);

            JsonObject request = new JsonObject();
            request.addProperty("jsonrpc", "2.0");
            request.addProperty("id", 1);
            request.addProperty("method", "initialize");
            transport.send(request);

            assertTrue(requestSeen.await(2, TimeUnit.SECONDS));
            assertEquals(1, posted.size());
            assertTrue(posted.get(0).contains("\"initialize\""));
            assertTrue(responseSeen.await(2, TimeUnit.SECONDS));
            assertEquals(1, received.get(0).get("id").getAsInt());
            transport.close();
        } finally {
            server.stop(0);
        }
    }
}
