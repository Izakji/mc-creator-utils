package com.izakji.agent.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.izakji.agent.api.ChatRequest;
import com.izakji.agent.api.TextPart;
import com.izakji.agent.api.UserMessage;
import com.izakji.agent.config.ProviderProfile;
import com.izakji.agent.config.ProviderType;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ProviderErrorTest {

    @Test
    void reportsTheProviderErrorBody() throws Exception {
        HttpServer server = server(exchange -> {
            byte[] body = "{\"error\":{\"message\":\"invalid api key\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(401, body.length);
            exchange.getResponseBody().write(body);
        });
        try {
            CompletionException failure = assertThrows(
                    CompletionException.class,
                    () -> new OpenAiCompatibleProvider()
                            .stream(
                                            request(profile(ProviderType.OPENAI_COMPATIBLE, base(server)), 2048),
                                            new RecordingListener(),
                                            () -> false)
                                    .join());
            assertTrue(failure.getCause().getMessage().contains("401"));
            assertTrue(failure.getCause().getMessage().contains("invalid api key"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void failsTheStreamOnAnOpenAiErrorChunk() throws Exception {
        try (TestHttpServer server = TestHttpServer.sse("""
                data: {"error":{"message":"quota exceeded"}}

                data: [DONE]

                """)) {
            CompletionException failure = assertThrows(
                    CompletionException.class,
                    () -> new OpenAiCompatibleProvider()
                            .stream(
                                            request(profile(ProviderType.OPENAI_COMPATIBLE, server.baseUrl()), 2048),
                                            new RecordingListener(),
                                            () -> false)
                                    .join());
            assertTrue(failure.getCause().getMessage().contains("quota exceeded"));
        }
    }

    @Test
    void failsTheStreamOnAnAnthropicErrorEvent() throws Exception {
        try (TestHttpServer server = TestHttpServer.sse("""
                event: error
                data: {"type":"error","error":{"message":"overloaded"}}

                """)) {
            CompletionException failure = assertThrows(
                    CompletionException.class,
                    () -> new AnthropicProvider()
                            .stream(
                                            request(profile(ProviderType.ANTHROPIC, server.baseUrl()), 2048),
                                            new RecordingListener(),
                                            () -> false)
                                    .join());
            assertTrue(failure.getCause().getMessage().contains("overloaded"));
        }
    }

    @Test
    void retriesOnceAfterATransientHttpFailure() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = server(exchange -> {
            if (requests.incrementAndGet() == 1) {
                byte[] body = "{\"error\":{\"message\":\"busy\"}}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.getResponseHeaders().add("Retry-After", "0");
                exchange.sendResponseHeaders(429, body.length);
                exchange.getResponseBody().write(body);
                return;
            }
            byte[] body = "data: {\"choices\":[{\"delta\":{\"content\":\"ok\"}}]}\n\ndata: [DONE]\n\n"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
        });
        try {
            RecordingListener listener = new RecordingListener();
            new OpenAiCompatibleProvider()
                    .stream(request(profile(ProviderType.OPENAI_COMPATIBLE, base(server)), 2048), listener, () -> false)
                            .join();
            assertEquals(2, requests.get());
            assertEquals("ok", listener.text.toString());
        } finally {
            server.stop(0);
        }
    }

    private static HttpServer server(com.sun.net.httpserver.HttpHandler handler) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            handler.handle(exchange);
            exchange.close();
        });
        server.start();
        return server;
    }

    private static String base(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static ProviderProfile profile(ProviderType type, String baseUrl) {
        return new ProviderProfile(
                "main", "Provider", type, baseUrl, "model", "sk-secret", Map.of(), 0.7, 2048, 128000, true, true);
    }

    private static ChatRequest request(ProviderProfile profile, int maxOutputTokens) {
        return new ChatRequest(
                profile, List.of(new UserMessage(List.of(new TextPart("hi")))), List.of(), maxOutputTokens);
    }
}
