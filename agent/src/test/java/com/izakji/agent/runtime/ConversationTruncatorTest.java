package com.izakji.agent.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.izakji.agent.api.AssistantMessage;
import com.izakji.agent.api.ChatMessage;
import com.izakji.agent.api.TextPart;
import com.izakji.agent.api.ToolMessage;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConversationTruncatorTest {

    @Test
    void neverSplitsToolCallFromItsResult() {
        List<ChatMessage> messages = List.of(
                TestMessages.user("old"),
                new AssistantMessage(List.of(), List.of(TestMessages.call("old-call"))),
                new ToolMessage("old-call", "mutate", List.of(new TextPart("result")), false),
                TestMessages.user("new"));
        List<ChatMessage> truncated = new ConversationTruncator(new TokenEstimator()).truncate(messages, 10);
        assertFalse(truncated.stream()
                .anyMatch(message -> message instanceof AssistantMessage assistant
                        && assistant.toolCalls().stream()
                                .anyMatch(call -> call.id().equals("old-call"))));
        assertFalse(truncated.stream()
                .anyMatch(message ->
                        message instanceof ToolMessage tool && tool.toolCallId().equals("old-call")));
        assertTrue(truncated.stream().anyMatch(message -> message instanceof com.izakji.agent.api.UserMessage));
    }

    @Test
    void reportsTruncationAndAddsANote() {
        List<ChatMessage> messages = List.of(
                TestMessages.user("old"),
                new AssistantMessage(List.of(), List.of(TestMessages.call("old-call"))),
                new ToolMessage("old-call", "mutate", List.of(new TextPart("result")), false),
                TestMessages.user("new"));
        ConversationTruncator.TruncationResult result =
                new ConversationTruncator(new TokenEstimator()).truncateDetailed(messages, 10);
        assertTrue(result.truncated());
        assertTrue(result.messages().get(0) instanceof com.izakji.agent.api.SystemMessage);
        assertTrue(result.messages().get(0).toString().contains("truncated"));
    }
}
