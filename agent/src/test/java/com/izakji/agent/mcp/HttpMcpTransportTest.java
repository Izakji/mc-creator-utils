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
    void dispatchesJsonResponsesEvenWhenTheirTextContainsData() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/mcp", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] body = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"text\":\"data: text/plain\"}}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            HttpMcpTransport transport = new HttpMcpTransport(config(server));
            List<JsonObject> received = new CopyOnWriteArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);
            transport
                    .start(json -> {
                        received.add(json);
                        latch.countDown();
                    })
                    .get(1, TimeUnit.SECONDS);
            transport.send(request(1));
            assertTrue(latch.await(1, TimeUnit.SECONDS));
            assertEquals(
                    "data: text/plain",
                    received.get(0).getAsJsonObject("result").get("text").getAsString());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void reportsHttpFailuresToThePendingRequest() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/mcp", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] body = "{\"error\":\"boom\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            HttpMcpTransport transport = new HttpMcpTransport(config(server));
            List<JsonObject> received = new CopyOnWriteArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);
            transport
                    .start(json -> {
                        received.add(json);
                        latch.countDown();
                    })
                    .get(1, TimeUnit.SECONDS);
            transport.send(request(7));
            assertTrue(latch.await(1, TimeUnit.SECONDS));
            assertEquals(7, received.get(0).get("id").getAsInt());
            assertTrue(received.get(0).has("error"));
        } finally {
            server.stop(0);
        }
    }

    private static McpServerConfig config(HttpServer server) {
        return new McpServerConfig(
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
    }

    private static JsonObject request(int id) {
        JsonObject request = new JsonObject();
        request.addProperty("jsonrpc", "2.0");
        request.addProperty("id", id);
        request.addProperty("method", "initialize");
        return request;
    }
}
