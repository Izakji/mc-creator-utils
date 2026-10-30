package com.izakji.agent.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import com.izakji.agent.api.ChatRequest;
import com.izakji.agent.api.TextPart;
import com.izakji.agent.api.ToolSchema;
import com.izakji.agent.api.UserMessage;
import com.izakji.agent.config.ProviderProfile;
import com.izakji.agent.config.ProviderType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleProviderTest {

    @Test
    void streamsTextAndToolCallsWithBearerAuth() throws Exception {
        try (TestHttpServer server = TestHttpServer.sse(
                """
                data: {"choices":[{"delta":{"content":"Hi"}}]}

                data: {"choices":[{"delta":{"tool_calls":[{"index":0,"id":"call-1","function":{"name":"game_snapshot","arguments":"{}"}}]}}]}

                data: {"choices":[{"delta":{},"finish_reason":"tool_calls"}]}

                data: [DONE]
                """)) {
            ProviderProfile profile = new ProviderProfile(
                    "main",
                    "OpenAI",
                    ProviderType.OPENAI_COMPATIBLE,
                    server.baseUrl(),
                    "gpt-4o-mini",
                    "sk-secret",
                    Map.of(),
                    0.7,
                    2048,
                    128000,
                    true,
                    true);
            RecordingListener listener = new RecordingListener();
            new OpenAiCompatibleProvider()
                    .stream(
                            new ChatRequest(
                                    profile,
                                    List.of(new UserMessage(List.of(new TextPart("hi")))),
                                    List.of(new ToolSchema("game_snapshot", "snapshot", new JsonObject())),
                                    2048),
                            listener,
                            () -> false)
                    .join();

            assertEquals("Hi", listener.text.toString());
            assertEquals("call-1", listener.toolCalls.get(0).id());
            assertEquals("Bearer sk-secret", server.lastAuthorization());
        }
    }
}
