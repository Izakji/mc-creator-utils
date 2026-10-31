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
import java.util.Map;
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
        assertFalse(session.ready());
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

    @Test
    void logoutStopsTheTurnAndClearsTheVisibleModel() {
        FakePort port = FakePort.withProfile();
        AgentChatSession session = new AgentChatSession(port, Runnable::run);
        session.presenter(new FakePresenter());
        session.newConversation("openai");
        session.send("hello");
        session.onLogout();
        assertFalse(session.busy());
        assertTrue(port.handle.isCancelled());
        assertTrue(session.model().entries().isEmpty());
        assertTrue(session.conversationId().isEmpty());
    }

    @Test
    void lateCallbacksFromStoppedTurnAreIgnored() {
        FakePort port = FakePort.withProfile();
        AgentChatSession session = new AgentChatSession(port, Runnable::run);
        session.presenter(new FakePresenter());
        session.newConversation("openai");
        session.send("first");
        TurnObserver old = port.observer;
        session.stop();
        old.onTextDelta("late");
        assertTrue(session.model().entries().stream()
                .noneMatch(entry -> entry.text().contains("late")));
    }

    @Test
    void explicitProfileSelectionIsUsedForNewConversation() {
        FakePort port = FakePort.withTwoProfiles();
        AgentChatSession session = new AgentChatSession(port, Runnable::run);
        session.presenter(new FakePresenter());
        assertTrue(session.selectProfile("deepseek"));
        assertTrue(session.send("hello"));
        assertEquals("deepseek", port.conversation.profileId());
    }

    @Test
    void selectingAProfileUpdatesAnExistingConversation() {
        FakePort port = FakePort.withTwoProfiles();
        String conversationId = port.storeConversation("openai");
        AgentChatSession session = new AgentChatSession(port, Runnable::run);
        session.presenter(new FakePresenter());
        assertTrue(session.open(conversationId));
        assertTrue(session.selectProfile("deepseek"));
        assertTrue(session.send("hello"));
        assertEquals("deepseek", port.conversation.profileId());
    }

    @Test
    void lateCallbacksAfterLogoutAreIgnored() {
        FakePort port = FakePort.withProfile();
        AgentChatSession session = new AgentChatSession(port, Runnable::run);
        session.presenter(new FakePresenter());
        session.newConversation("openai");
        session.send("first");
        TurnObserver old = port.observer;
        session.onLogout();
        old.onTextDelta("late");
        old.onApprovalNeeded(
                new ApprovalRequest("approval-1", "run_command", ToolRisk.MUTATING, new JsonObject(), "Run"));
        assertTrue(session.model().entries().isEmpty());
    }

    @Test
    void switchingConversationCancelsThePreviousTurn() {
        FakePort port = FakePort.withProfile();
        AgentChatSession session = new AgentChatSession(port, Runnable::run);
        session.presenter(new FakePresenter());
        session.newConversation("openai");
        session.send("first");
        TurnObserver old = port.observer;
        session.newConversation("openai");
        old.onTextDelta("late");
        assertTrue(port.handle.isCancelled());
        assertTrue(session.model().entries().stream()
                .noneMatch(entry -> entry.text().contains("late")));
    }

    @Test
    void sendWhilePreviousTurnStopsIsRejectedWithGuidance() {
        FakePort port = FakePort.withProfile();
        AgentChatSession session = new AgentChatSession(port, Runnable::run);
        session.presenter(new FakePresenter());
        session.newConversation("openai");
        session.send("first");
        session.stop();
        assertFalse(session.send("second"));
        assertEquals(1, port.started);
        assertTrue(session.model().entries().stream().anyMatch(entry -> entry.kind() == AgentChatModel.Kind.ERROR));
    }

    @Test
    void errorsAreNotDuplicatedOnFinish() {
        FakePort port = FakePort.withProfile();
        AgentChatSession session = new AgentChatSession(port, Runnable::run);
        session.presenter(new FakePresenter());
        session.newConversation("openai");
        session.send("hello");
        port.observer.onError(new com.izakji.agent.api.AgentException("boom", null));
        port.observer.onFinished(new com.izakji.agent.runtime.TurnResult(
                com.izakji.agent.runtime.TurnOutcome.ERROR, session.conversationId(), "boom"));
        assertEquals(
                1,
                session.model().entries().stream()
                        .filter(entry -> entry.kind() == AgentChatModel.Kind.ERROR)
                        .count());
    }

    @Test
    void blankRemoteKeyReturnsAnActionableErrorBeforeStarting() {
        FakePort port = FakePort.withBlankKeyProfile();
        AgentChatSession session = new AgentChatSession(port, Runnable::run);
        session.presenter(new FakePresenter());
        assertTrue(session.newConversation("openai"));
        assertFalse(session.send("hello"));
        assertEquals(0, port.started);
        assertTrue(session.model().entries().stream()
                .anyMatch(entry -> entry.kind() == AgentChatModel.Kind.ERROR
                        && entry.text().contains("API key")));
    }

    @Test
    void deletedConversationProfileDoesNotSilentlyCreateANewChat() {
        FakePort port = FakePort.withProfile();
        String conversationId = port.storeConversation("missing");
        AgentChatSession session = new AgentChatSession(port, Runnable::run);
        session.presenter(new FakePresenter());
        assertTrue(session.open(conversationId));
        assertFalse(session.send("hello"));
        assertEquals(0, port.started);
        assertEquals(conversationId, session.conversationId());
        assertTrue(session.model().entries().stream()
                .anyMatch(entry -> entry.kind() == AgentChatModel.Kind.ERROR
                        && entry.text().contains("profile")));
    }

    @Test
    void denyingAnApprovalClearsTheCardAndKeepsTheTurnUsable() {
        FakePort port = FakePort.withProfile();
        AgentChatSession session = new AgentChatSession(port, Runnable::run);
        session.presenter(new FakePresenter());
        session.newConversation("openai");
        session.send("hello");
        port.observer.onApprovalNeeded(
                new ApprovalRequest("approval-1", "run_command", ToolRisk.MUTATING, new JsonObject(), "Run"));
        session.approve("approval-1", ApprovalChoice.DENY);
        assertTrue(session.model().pendingApproval().isEmpty());
        port.observer.onFinished(new com.izakji.agent.runtime.TurnResult(
                com.izakji.agent.runtime.TurnOutcome.COMPLETED, session.conversationId(), ""));
        assertFalse(session.busy());
    }

    @Test
    void approvalTimeoutResultClearsTheCardAndKeepsTheTurnUsable() {
        FakePort port = FakePort.withProfile();
        AgentChatSession session = new AgentChatSession(port, Runnable::run);
        session.presenter(new FakePresenter());
        session.newConversation("openai");
        session.send("hello");
        port.observer.onApprovalNeeded(
                new ApprovalRequest("approval-1", "run_command", ToolRisk.MUTATING, new JsonObject(), "Run"));
        port.observer.onToolFinished(
                new com.izakji.agent.api.ToolResult("call-1", "Permission denied", true, List.of()));
        assertTrue(session.model().pendingApproval().isEmpty());
        port.observer.onFinished(new com.izakji.agent.runtime.TurnResult(
                com.izakji.agent.runtime.TurnOutcome.COMPLETED, session.conversationId(), ""));
        assertFalse(session.busy());
        assertTrue(session.model().entries().stream().noneMatch(entry -> entry.kind() == AgentChatModel.Kind.APPROVAL));
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
            return new FakePort(new AgentSettings(
                    "openai",
                    List.of(profile("openai", "OpenAI", "gpt-4o-mini", "sk-test")),
                    new PermissionRules(true, Set.of(), Set.of(), Set.of()),
                    8,
                    32,
                    1));
        }

        static FakePort withTwoProfiles() {
            return new FakePort(new AgentSettings(
                    "openai",
                    List.of(
                            profile("openai", "OpenAI", "gpt-4o-mini", ""),
                            profile("deepseek", "DeepSeek", "deepseek-flash", "sk-deepseek")),
                    new PermissionRules(true, Set.of(), Set.of(), Set.of()),
                    8,
                    32,
                    1));
        }

        static FakePort withBlankKeyProfile() {
            return new FakePort(AgentDefaults.settings());
        }

        private static com.izakji.agent.config.ProviderProfile profile(
                String id, String name, String model, String key) {
            return new com.izakji.agent.config.ProviderProfile(
                    id,
                    name,
                    com.izakji.agent.config.ProviderType.OPENAI_COMPATIBLE,
                    "https://api.openai.com/v1",
                    model,
                    key,
                    Map.of(),
                    0.7,
                    2048,
                    128000,
                    true,
                    true);
        }

        static FakePort withoutProfile() {
            return new FakePort(new AgentSettings(
                    "", List.of(), new PermissionRules(true, Set.of(), Set.of(), Set.of()), 8, 32, 1));
        }

        String storeConversation(String profileId) {
            Conversation conversation = conversations.create("Old Chat", profileId);
            conversations.save(conversation);
            return conversation.id();
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
