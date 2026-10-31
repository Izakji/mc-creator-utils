package com.izakji.mccreatorutils.client.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.izakji.agent.api.AssistantMessage;
import com.izakji.agent.api.ReasoningPart;
import com.izakji.agent.api.TextPart;
import com.izakji.agent.api.ToolMessage;
import com.izakji.agent.api.ToolRisk;
import com.izakji.agent.api.UserMessage;
import com.izakji.agent.conversation.Conversation;
import com.izakji.agent.runtime.ApprovalRequest;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AgentChatModelTest {

    @Test
    void mergesStreamingDeltasIntoOneAssistantEntry() {
        AgentChatModel model = new AgentChatModel();
        model.appendAssistant("Hel");
        model.appendAssistant("lo");
        assertEquals(1, model.entries().size());
        assertEquals("Hello", model.entries().get(0).text());
        assertEquals(AgentChatModel.Kind.ASSISTANT, model.entries().get(0).kind());
    }

    @Test
    void loadsPersistedMessagesAndHidesToolActivity() {
        Conversation conversation = new Conversation(
                "id",
                "Chat",
                Instant.now(),
                Instant.now(),
                "openai",
                List.of(
                        new UserMessage(List.of(new TextPart("hi"))),
                        new AssistantMessage(
                                List.of(new ReasoningPart("why", "sig"), new TextPart("answer")), List.of()),
                        new ToolMessage("call-1", "read", List.of(new TextPart("boom")), true)),
                Set.of());
        AgentChatModel model = new AgentChatModel();
        model.load(conversation);
        assertEquals(
                List.of(AgentChatModel.Kind.USER, AgentChatModel.Kind.ASSISTANT),
                model.entries().stream().map(AgentChatModel.Entry::kind).toList());
    }

    @Test
    void labelsUserMessagesWithThePlayerName() {
        AgentChatModel model = new AgentChatModel();
        model.setUserName("Steve");
        model.addUser("hi");
        assertEquals("Steve", model.entries().get(0).title());
    }

    @Test
    void keepsPendingApprovalUntilCleared() {
        AgentChatModel model = new AgentChatModel();
        model.addApproval(
                new ApprovalRequest("approval-1", "run_command", ToolRisk.MUTATING, new JsonObject(), "Run command"));
        assertEquals("approval-1", model.pendingApproval().orElseThrow().id());
        model.clearApproval();
        assertTrue(model.pendingApproval().isEmpty());
        assertTrue(model.entries().stream().noneMatch(entry -> entry.kind() == AgentChatModel.Kind.APPROVAL));
        model.clear();
        assertTrue(model.entries().isEmpty());
    }

    @Test
    void incrementsRevisionOnEveryMutation() {
        AgentChatModel model = new AgentChatModel();
        long before = model.revision();
        model.addUser("hi");
        assertTrue(model.revision() > before);
    }
}
