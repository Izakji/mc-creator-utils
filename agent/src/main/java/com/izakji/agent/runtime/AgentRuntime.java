package com.izakji.agent.runtime;

import com.izakji.agent.api.AgentException;
import com.izakji.agent.api.AssistantMessage;
import com.izakji.agent.api.ChatMessage;
import com.izakji.agent.api.ChatRequest;
import com.izakji.agent.api.ContentPart;
import com.izakji.agent.api.LlmProvider;
import com.izakji.agent.api.ReasoningPart;
import com.izakji.agent.api.StreamListener;
import com.izakji.agent.api.SystemMessage;
import com.izakji.agent.api.TextPart;
import com.izakji.agent.api.ToolCall;
import com.izakji.agent.api.ToolContext;
import com.izakji.agent.api.UserMessage;
import com.izakji.agent.config.AgentSettings;
import com.izakji.agent.config.ProviderProfile;
import com.izakji.agent.conversation.Conversation;
import com.izakji.agent.conversation.ConversationStore;
import com.izakji.agent.provider.ProviderRegistry;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Loader-neutral agent turn runtime.
 *
 * @author Izakji
 */
public final class AgentRuntime implements AutoCloseable {
    private final AgentSettings settings;
    private final ProviderRegistry providers;
    private final ToolRegistry tools;
    private final ConversationStore conversations;
    private final PermissionGate permissions;
    private final List<ContextProvider> contextProviders;
    private final String agentsMarkdown;
    private final ExecutorService executor;
    private final Clock clock;
    private final Map<String, TurnHandle> active = new ConcurrentHashMap<>();
    private final SystemPromptBuilder promptBuilder = new SystemPromptBuilder();

    public AgentRuntime(
            AgentSettings settings,
            ProviderRegistry providers,
            ToolRegistry tools,
            ConversationStore conversations,
            PermissionGate permissions,
            List<ContextProvider> contextProviders,
            String agentsMarkdown,
            ExecutorService executor,
            Clock clock) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.providers = Objects.requireNonNull(providers, "providers");
        this.tools = Objects.requireNonNull(tools, "tools");
        this.conversations = Objects.requireNonNull(conversations, "conversations");
        this.permissions = Objects.requireNonNull(permissions, "permissions");
        this.contextProviders = List.copyOf(Objects.requireNonNullElse(contextProviders, List.of()));
        this.agentsMarkdown = agentsMarkdown == null ? "" : agentsMarkdown;
        this.executor = Objects.requireNonNull(executor, "executor");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Conversation createConversation(String title, String profileId) {
        Conversation conversation = conversations.create(title, profileId);
        conversations.save(conversation);
        return conversation;
    }

    public TurnHandle startTurn(String conversationId, String userText, TurnObserver observer) {
        Conversation conversation = conversations.load(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown conversation: " + conversationId));
        TurnHandle handle = new TurnHandle();
        if (active.putIfAbsent(conversationId, handle) != null) {
            throw new IllegalStateException("A turn is already running for this conversation");
        }
        handle.setCancelAction(() -> cancelTurn(conversationId, handle));
        executor.execute(() -> runTurn(conversation, userText, observer, handle));
        return handle;
    }

    public void approve(String conversationId, String approvalId, ApprovalChoice choice) {
        // Approval resolution is wired in Task 10; an unknown approval is a no-op.
    }

    public void close() {
        executor.shutdownNow();
    }

    ConversationStore conversations() {
        return conversations;
    }

    AgentSettings settings() {
        return settings;
    }

    private void runTurn(
            Conversation conversation, String userText, TurnObserver observer, TurnHandle handle) {
        String conversationId = conversation.id();
        try {
            if (handle.isCancelled()) {
                finish(conversationId, handle, observer, TurnOutcome.CANCELLED, "cancelled");
                return;
            }
            InstantHolder now = new InstantHolder(clock.instant());
            Conversation updated = conversation.withMessage(
                    new UserMessage(List.of(new TextPart(userText))), now.value);
            conversations.save(updated);

            ProviderProfile profile = settings.profiles().stream()
                    .filter(candidate -> candidate.id().equals(updated.profileId()))
                    .findFirst()
                    .orElseThrow(() -> new AgentException("Provider profile not found: " + updated.profileId()));
            LlmProvider provider = providers.forType(profile.type())
                    .orElseThrow(() -> new AgentException("Provider not registered: " + profile.type()));

            String systemPrompt = promptBuilder.build(
                    settings, List.of(), agentsMarkdown, snapshots(conversationId, handle));
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new SystemMessage(systemPrompt));
            messages.addAll(updated.messages());
            ChatRequest request = new ChatRequest(
                    profile, messages, tools.schemas(), profile.maxOutputTokens());

            StringBuilder text = new StringBuilder();
            StringBuilder reasoning = new StringBuilder();
            AtomicReference<String> reasoningSignature = new AtomicReference<>("");
            StreamListener listener = new StreamListener() {
                @Override
                public void onTextDelta(String delta) {
                    text.append(delta);
                    observer.onTextDelta(delta);
                }

                @Override
                public void onReasoningDelta(String delta) {
                    reasoning.append(delta);
                    observer.onReasoningDelta(delta);
                }

                @Override
                public void onReasoningSignature(String signature) {
                    reasoningSignature.set(signature);
                }

                @Override
                public void onToolCall(ToolCall call) {
                    // Tool execution is added in Task 10.
                }

                @Override
                public void onUsage(int promptTokens, int completionTokens) {}

                @Override
                public void onFinish(com.izakji.agent.api.FinishReason reason) {}
            };

            provider.stream(request, listener, handle::isCancelled).join();
            if (handle.isCancelled()) {
                finish(conversationId, handle, observer, TurnOutcome.CANCELLED, "cancelled");
                return;
            }
            List<ContentPart> parts = new ArrayList<>();
            if (!reasoning.isEmpty()) {
                parts.add(new ReasoningPart(reasoning.toString(), reasoningSignature.get()));
            }
            if (!text.isEmpty()) {
                parts.add(new TextPart(text.toString()));
            }
            Conversation completed = updated.withMessage(
                    new AssistantMessage(parts, List.of()), clock.instant());
            conversations.save(completed);
            finish(conversationId, handle, observer, TurnOutcome.COMPLETED, "");
        } catch (Exception failure) {
            String message = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
            observer.onError(new AgentException("Provider failed: " + message, failure));
            finish(conversationId, handle, observer, TurnOutcome.ERROR, message);
        }
    }

    private List<String> snapshots(String conversationId, TurnHandle handle) {
        ToolContext context = new ToolContext(conversationId, handle::isCancelled);
        List<String> values = new ArrayList<>();
        for (ContextProvider provider : contextProviders) {
            try {
                String value = provider.snapshot(context)
                        .orTimeout(2, TimeUnit.SECONDS)
                        .exceptionally(failure -> "")
                        .join();
                if (value != null && !value.isBlank()) {
                    values.add(value);
                }
            } catch (RuntimeException ignored) {
                // A failed optional context provider must not fail the turn.
            }
        }
        return values;
    }

    private void cancelTurn(String conversationId, TurnHandle handle) {
        if (active.get(conversationId) == handle) {
            handle.complete(new TurnResult(TurnOutcome.CANCELLED, conversationId, "cancelled"));
        }
    }

    private void finish(
            String conversationId,
            TurnHandle handle,
            TurnObserver observer,
            TurnOutcome outcome,
            String error) {
        active.remove(conversationId, handle);
        TurnResult result = new TurnResult(outcome, conversationId, error);
        observer.onFinished(result);
        handle.complete(result);
    }

    private static final class InstantHolder {
        private final java.time.Instant value;

        private InstantHolder(java.time.Instant value) {
            this.value = value;
        }
    }
}
