package com.izakji.agent.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.izakji.agent.api.AssistantMessage;
import com.izakji.agent.api.CancellationToken;
import com.izakji.agent.api.ChatRequest;
import com.izakji.agent.api.FinishReason;
import com.izakji.agent.api.LlmProvider;
import com.izakji.agent.api.ReasoningPart;
import com.izakji.agent.api.StreamListener;
import com.izakji.agent.config.ProviderType;
import com.izakji.agent.conversation.Conversation;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class AgentRuntimeReasoningTest {

    @Test
    void keepsOneReasoningPartPerSignedBlock() throws Exception {
        LlmProvider provider = new LlmProvider() {
            @Override
            public ProviderType type() {
                return ProviderType.OPENAI_COMPATIBLE;
            }

            @Override
            public boolean supportsTools() {
                return false;
            }

            @Override
            public CompletableFuture<Void> stream(
                    ChatRequest request, StreamListener listener, CancellationToken cancellationToken) {
                listener.onReasoningDelta("first");
                listener.onReasoningSignature("sig-1");
                listener.onReasoningDelta("second");
                listener.onReasoningSignature("sig-2");
                listener.onFinish(FinishReason.STOP);
                return CompletableFuture.completedFuture(null);
            }
        };
        try (AgentRuntime runtime =
                AgentRuntimeTestSupport.runtime(AgentRuntimeTestSupport.root(), provider, List.of())) {
            Conversation conversation = runtime.createConversation("Chat", "main");
            runtime.startTurn(conversation.id(), "think", new RecordingObserver())
                    .completion()
                    .get(2, TimeUnit.SECONDS);
            Conversation loaded =
                    runtime.conversations().load(conversation.id()).orElseThrow();
            List<ReasoningPart> parts = loaded.messages().stream()
                    .filter(AssistantMessage.class::isInstance)
                    .map(AssistantMessage.class::cast)
                    .flatMap(message -> message.parts().stream())
                    .filter(ReasoningPart.class::isInstance)
                    .map(ReasoningPart.class::cast)
                    .toList();
            assertEquals(2, parts.size());
            assertEquals("first", parts.get(0).text());
            assertEquals("sig-1", parts.get(0).signature());
            assertEquals("second", parts.get(1).text());
            assertEquals("sig-2", parts.get(1).signature());
        }
    }
}
