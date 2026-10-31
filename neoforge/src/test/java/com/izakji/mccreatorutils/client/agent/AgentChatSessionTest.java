package com.izakji.mccreatorutils.client.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.izakji.agent.api.ToolRisk;
import com.izakji.agent.config.AgentSettings;
import com.izakji.agent.config.PermissionRules;
import com.izakji.agent.conversation.Conversation;
import com.izakji.agent.conversation.ConversationStore;
import com.izakji.agent.conversation.ConversationSummary;
import com.izakji.agent.runtime.ApprovalChoice;
import com.izakji.agent.runtime.ApprovalRequest;
import com.izakji.agent.runtime.TurnHandle;
import com.izakji.agent.runtime.TurnObserver;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AgentChatSessionTest {

    @Test
    void refusesASecondActiveTurn() {
        FakePort port = FakePort.withProfile();
        AgentChatSession session = new AgentChatSession(port, Runnable::run);
        session.presenter(new FakePresenter());
        assertTrue(session.newConversation("openai"));
        assertTrue(session.send("first"));
        assertFalse(session.send("second"));
        assertEquals(1, port.started);
        assertEquals("first", session.model().entries().get(0).text());
    }

    @Test
    void approvalRequestsFocusAndKeepsTheCard() {
        FakePort port = FakePort.withProfile();
        AgentChatSession session = new AgentChatSession(port, Runnable::run);
        FakePresenter presenter = new FakePresenter();
        session.presenter(presenter);
        session.newConversation("openai");
        session.send("go");
        port.observer.onApprovalNeeded(
                new ApprovalRequest("approval-1", "run_command", ToolRisk.MUTATING, new JsonObject(), "Run"));
        assertTrue(presenter.focused);
        assertEquals(
                "approval-1", session.model().pendingApproval().orElseThrow().id());
    }

    @Test
    void stopClearsBusyAndRetainsVisibleText() {
        FakePort port = FakePort.withProfile();
        AgentChatSession session = new AgentChatSession(port, Runnable::run);
        session.presenter(new FakePresenter());
        session.newConversation("openai");
        session.send("hello");
        port.observer.onTextDelta("partial");
        session.stop();
        assertFalse(session.busy());
        assertEquals("partial", session.model().entries().get(1).text());
        assertTrue(port.handle.isCancelled());
    }

    @Test
    void missingProfileReturnsAUsefulError() {
        FakePort port = FakePort.withoutProfile();
        AgentChatSession session = new AgentChatSession(port, Runnable::run);
        session.presenter(new FakePresenter());
        assertFalse(session.send("hello"));
        assertTrue(session.model().entries().stream().anyMatch(entry -> entry.kind() == AgentChatModel.Kind.ERROR));
    }

    @Test
    void titlesANewConversationFromTheFirstMessage() {
        FakePort port = FakePort.withProfile();
        AgentChatSession session = new AgentChatSession(port, Runnable::run);
        session.presenter(new FakePresenter());
        session.newConversation("openai");
        session.send("Find diamonds\nplease");
        assertEquals("Find diamonds", port.conversation.title());
    }

    @Test
    void finishedTurnClearsBusy() {
        FakePort port = FakePort.withProfile();
        AgentChatSession session = new AgentChatSession(port, Runnable::run);
        session.presenter(new FakePresenter());
        session.newConversation("openai");
        session.send("hello");
        port.observer.onFinished(new com.izakji.agent.runtime.TurnResult(
                com.izakji.agent.runtime.TurnOutcome.COMPLETED, session.conversationId(), ""));
        assertFalse(session.busy());
    }

    private static final class FakePresenter implements AgentChatPresenter {
        private boolean focused;
        private int refreshes;

        @Override
        public void requestFocus() {
            focused = true;
        }

        @Override
        public void refresh() {
            refreshes++;
        }
    }

    private static final class FakePort implements AgentTurnPort {
        private final ConversationStore conversations = new MemoryConversationStore();
        private final AgentSettings settings;
        private TurnObserver observer;
        private TurnHandle handle;
        private int started;
        private Conversation conversation;

        private FakePort(AgentSettings settings) {
            this.settings = settings;
        }

        static FakePort withProfile() {
            return new FakePort(AgentDefaults.settings());
        }

        static FakePort withoutProfile() {
            return new FakePort(new AgentSettings(
                    "", List.of(), new PermissionRules(true, Set.of(), Set.of(), Set.of()), 8, 32, 1));
        }

        @Override
        public AgentSettings settings() {
            return settings;
        }

        @Override
        public ConversationStore conversations() {
            return conversations;
        }

        @Override
        public TurnHandle startTurn(String conversationId, String userText, TurnObserver observer) {
            started++;
            this.observer = observer;
            this.conversation = conversations.load(conversationId).orElseThrow();
            this.handle = new TurnHandle();
            return handle;
        }

        @Override
        public void approve(String conversationId, String approvalId, ApprovalChoice choice) {}
    }

    private static final class MemoryConversationStore implements ConversationStore {
        private final java.util.Map<String, Conversation> values = new java.util.HashMap<>();

        @Override
        public Conversation create(String title, String profileId) {
            Instant now = Instant.now();
            return new Conversation(UUID.randomUUID().toString(), title, now, now, profileId, List.of(), Set.of());
        }

        @Override
        public Optional<Conversation> load(String id) {
            return Optional.ofNullable(values.get(id));
        }

        @Override
        public void save(Conversation conversation) {
            values.put(conversation.id(), conversation);
        }

        @Override
        public List<ConversationSummary> list() {
            List<ConversationSummary> result = new ArrayList<>();
            values.values()
                    .forEach(conversation -> result.add(
                            new ConversationSummary(conversation.id(), conversation.title(), conversation.updated())));
            result.sort(Comparator.comparing(ConversationSummary::updated).reversed());
            return result;
        }

        @Override
        public void delete(String id) {
            values.remove(id);
        }

        @Override
        public void rename(String id, String title) {
            Conversation conversation = values.get(id);
            if (conversation != null) {
                values.put(id, conversation.withTitle(title, Instant.now()));
            }
        }
    }
}
