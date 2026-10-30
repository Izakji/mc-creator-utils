package com.izakji.agent.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.izakji.agent.api.AssistantMessage;
import com.izakji.agent.conversation.Conversation;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class AgentRuntimeTextTest {

    @Test
    void streamsTextAndPersistsAssistantMessage() throws Exception {
        Path root = AgentRuntimeTestSupport.root();
        FakeProvider provider =
                new FakeProvider(List.of(new FakeProvider.Turn(List.of("Hello ", "world"), List.of(), List.of())));
        try (AgentRuntime runtime = AgentRuntimeTestSupport.runtime(root, provider, List.of())) {
            Conversation conversation = runtime.createConversation("Chat", "main");
            RecordingObserver observer = new RecordingObserver();
            TurnResult result = runtime.startTurn(conversation.id(), "hi", observer)
                    .completion()
                    .get(2, TimeUnit.SECONDS);

            assertEquals(TurnOutcome.COMPLETED, result.outcome());
            assertEquals("Hello world", observer.text.toString());
            Conversation loaded =
                    runtime.conversations().load(conversation.id()).orElseThrow();
            assertEquals(2, loaded.messages().size());
            assertTrue(loaded.messages().get(1) instanceof AssistantMessage);
        }
    }

    @Test
    void providerFailureFinishesWithError() throws Exception {
        Path root = AgentRuntimeTestSupport.root();
        FakeProvider provider = new FakeProvider(List.of(new FakeProvider.Failure("boom")));
        try (AgentRuntime runtime = AgentRuntimeTestSupport.runtime(root, provider, List.of())) {
            Conversation conversation = runtime.createConversation("Chat", "main");
            RecordingObserver observer = new RecordingObserver();
            TurnResult result = runtime.startTurn(conversation.id(), "hi", observer)
                    .completion()
                    .get(2, TimeUnit.SECONDS);
            assertEquals(TurnOutcome.ERROR, result.outcome());
            assertTrue(observer.errors.get(0).getMessage().contains("boom"));
        }
    }
}
