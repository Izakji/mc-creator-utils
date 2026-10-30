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

class AnthropicProviderTest {

    @Test
    void streamsTextToolUseAndPreservesThinkingSignature() throws Exception {
        try (TestHttpServer server = TestHttpServer.sse("""
                event: content_block_start
                data: {"content_block":{"type":"thinking","thinking":"","signature":"sig"}}

                event: content_block_delta
                data: {"delta":{"type":"thinking_delta","thinking":"plan"}}

                event: content_block_stop
                data: {}

                event: content_block_start
                data: {"content_block":{"type":"text","text":""}}

                event: content_block_delta
                data: {"delta":{"type":"text_delta","text":"Hi"}}

                event: content_block_stop
                data: {}

                event: content_block_start
                data: {"content_block":{"type":"tool_use","id":"call-1","name":"game_snapshot"}}

                event: content_block_delta
                data: {"delta":{"type":"input_json_delta","partial_json":"{}"}}

                event: content_block_stop
                data: {}

                event: message_stop
                data: {}
                """)) {
            ProviderProfile profile = new ProviderProfile(
                    "main",
                    "Anthropic",
                    ProviderType.ANTHROPIC,
                    server.baseUrl(),
                    "claude-sonnet-4-5",
                    "sk-ant",
                    Map.of(),
                    0.7,
                    2048,
                    128000,
                    true,
                    true);
            RecordingListener listener = new RecordingListener();
            new AnthropicProvider()
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
            assertEquals("plan", listener.reasoning.toString());
            assertEquals("sig", listener.signature);
            assertEquals("call-1", listener.toolCalls.get(0).id());
            assertEquals("sk-ant", server.lastApiKey());
            assertEquals("2023-06-01", server.lastAnthropicVersion());
        }
    }

    @Test
    void sendsThinkingBudgetAndOnlyReplaysSignedOrRedactedBlocks() throws Exception {
        try (TestHttpServer server = TestHttpServer.sse("""
                event: message_stop
                data: {}

                """)) {
            ProviderProfile profile = new ProviderProfile(
                    "main",
                    "Anthropic",
                    ProviderType.ANTHROPIC,
                    server.baseUrl(),
                    "claude-sonnet-4-5",
                    "sk-ant",
                    Map.of(),
                    0.7,
                    2048,
                    128000,
                    true,
                    true,
                    1024);
            com.izakji.agent.api.AssistantMessage assistant = new com.izakji.agent.api.AssistantMessage(
                    List.of(
                            new com.izakji.agent.api.ReasoningPart("unsigned", ""),
                            new com.izakji.agent.api.ReasoningPart("signed", "sig"),
                            new com.izakji.agent.api.ReasoningPart("", "redacted:opaque")),
                    List.of());
            new AnthropicProvider()
                    .stream(
                                    new ChatRequest(
                                            profile,
                                            List.of(new UserMessage(List.of(new TextPart("hi"))), assistant),
                                            List.of(),
                                            2048),
                                    new RecordingListener(),
                                    () -> false)
                            .join();

            String body = server.lastBody();
            assertEquals(true, body.contains("\"budget_tokens\":1024"));
            assertEquals(true, body.contains("\"signature\":\"sig\""));
            assertEquals(false, body.contains("unsigned"));
            assertEquals(true, body.contains("\"type\":\"redacted_thinking\""));
            assertEquals(true, body.contains("\"data\":\"opaque\""));
        }
    }
}
