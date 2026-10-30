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
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ProviderStreamingTest {

    @Test
    void deliversDeltasBeforeTheResponseEnds() throws Exception {
        try (SlowSseServer server = new SlowSseServer(
                "data: {\"choices\":[{\"delta\":{\"content\":\"first\"}}]}\n\n",
                "data: {\"choices\":[{\"delta\":{\"content\":\"second\"}}]}\n\ndata: [DONE]\n\n")) {
            CountDownLatch firstDelta = new CountDownLatch(1);
            RecordingListener listener = new RecordingListener() {
                @Override
                public void onTextDelta(String delta) {
                    super.onTextDelta(delta);
                    firstDelta.countDown();
                }
            };
            CompletableFuture<Void> future =
                    provider(server.baseUrl()).stream(request(server.baseUrl()), listener, () -> false);
            assertTrue(firstDelta.await(1, TimeUnit.SECONDS));
            server.release();
            future.get(2, TimeUnit.SECONDS);
            assertEquals("firstsecond", listener.text.toString());
        }
    }

    @Test
    void cancellingTheResponseClosesTheServerStream() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        CountDownLatch disconnected = new CountDownLatch(1);
        server.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            try {
                while (true) {
                    exchange.getResponseBody()
                            .write("data: {\"choices\":[{\"delta\":{\"content\":\"tick\"}}]}\n\n"
                                    .getBytes(StandardCharsets.UTF_8));
                    exchange.getResponseBody().flush();
                    Thread.sleep(20);
                }
            } catch (Exception closed) {
                disconnected.countDown();
            }
            exchange.close();
        });
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            CompletableFuture<Void> future =
                    provider(baseUrl).stream(request(baseUrl), new RecordingListener(), () -> false);
            Thread.sleep(100);
            future.cancel(true);
            assertTrue(disconnected.await(2, TimeUnit.SECONDS));
            assertThrows(CancellationException.class, () -> future.get(1, TimeUnit.SECONDS));
        } finally {
            server.stop(0);
        }
    }

    private static OpenAiCompatibleProvider provider(String baseUrl) {
        return new OpenAiCompatibleProvider();
    }

    private static ChatRequest request(String baseUrl) {
        ProviderProfile profile = new ProviderProfile(
                "main",
                "OpenAI",
                ProviderType.OPENAI_COMPATIBLE,
                baseUrl,
                "gpt-4o-mini",
                "sk-secret",
                Map.of(),
                0.7,
                2048,
                128000,
                true,
                true);
        return new ChatRequest(profile, List.of(new UserMessage(List.of(new TextPart("hi")))), List.of(), 2048);
    }
}
