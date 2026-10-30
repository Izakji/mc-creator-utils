package com.izakji.agent.provider;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

final class TestHttpServer implements AutoCloseable {
    private final HttpServer server;
    private volatile String lastAuthorization = "";
    private volatile String lastApiKey = "";
    private volatile String lastAnthropicVersion = "";

    private TestHttpServer(String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            lastAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
            lastApiKey = exchange.getRequestHeaders().getFirst("x-api-key");
            lastAnthropicVersion = exchange.getRequestHeaders().getFirst("anthropic-version");
            exchange.getRequestBody().readAllBytes();
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
    }

    static TestHttpServer sse(String body) throws IOException {
        return new TestHttpServer(body);
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    String lastAuthorization() {
        return lastAuthorization;
    }

    String lastApiKey() {
        return lastApiKey;
    }

    String lastAnthropicVersion() {
        return lastAnthropicVersion;
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
