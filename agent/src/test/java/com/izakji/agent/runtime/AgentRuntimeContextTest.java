package com.izakji.agent.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.izakji.agent.api.SystemMessage;
import com.izakji.agent.conversation.Conversation;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class AgentRuntimeContextTest {

    @Test
    void redactsConfiguredSecretsFromContextSnapshots() throws Exception {
        FakeProvider provider = new FakeProvider(List.of(new FakeProvider.Turn(List.of("done"), List.of(), List.of())));
        ContextProvider context = new ContextProvider() {
            @Override
            public String id() {
                return "game";
            }

            @Override
            public CompletableFuture<String> snapshot(com.izakji.agent.api.ToolContext toolContext) {
                return CompletableFuture.completedFuture("authorization sk-secret");
            }
        };
        try (AgentRuntime runtime = AgentRuntimeTestSupport.runtime(
                AgentRuntimeTestSupport.root(),
                provider,
                List.of(),
                AgentRuntimeTestSupport.settings(),
                List.of(context),
                List.of())) {
            Conversation conversation = runtime.createConversation("Chat", "main");
            runtime.startTurn(conversation.id(), "hello", new RecordingObserver())
                    .completion()
                    .get(2, TimeUnit.SECONDS);
            String prompt = systemPrompt(provider);
            assertFalse(prompt.contains("sk-secret"));
            assertTrue(prompt.contains("[REDACTED]"));
        }
    }

    @Test
    void redactsAdditionalMcpSecretsFromContextSnapshots() throws Exception {
        FakeProvider provider = new FakeProvider(List.of(new FakeProvider.Turn(List.of("done"), List.of(), List.of())));
        ContextProvider context = new ContextProvider() {
            @Override
            public String id() {
                return "game";
            }

            @Override
            public CompletableFuture<String> snapshot(com.izakji.agent.api.ToolContext toolContext) {
                return CompletableFuture.completedFuture("mcp-token=abc123");
            }
        };
        try (AgentRuntime runtime = AgentRuntimeTestSupport.runtime(
                AgentRuntimeTestSupport.root(),
                provider,
                List.of(),
                AgentRuntimeTestSupport.settings(),
                List.of(context),
                List.of("abc123"))) {
            Conversation conversation = runtime.createConversation("Chat", "main");
            runtime.startTurn(conversation.id(), "hello", new RecordingObserver())
                    .completion()
                    .get(2, TimeUnit.SECONDS);
            assertFalse(systemPrompt(provider).contains("abc123"));
        }
    }

    private static String systemPrompt(FakeProvider provider) {
        return provider.lastMessages().stream()
                .filter(SystemMessage.class::isInstance)
                .map(SystemMessage.class::cast)
                .map(SystemMessage::text)
                .findFirst()
                .orElseThrow();
    }
}
