package com.izakji.agent.runtime;

import com.izakji.agent.api.AgentException;
import com.izakji.agent.api.AgentTool;
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
import com.izakji.agent.api.ToolResult;
import com.izakji.agent.api.ToolSchema;
import com.izakji.agent.api.UserMessage;
import com.izakji.agent.config.AgentSettings;
import com.izakji.agent.config.ProviderProfile;
import com.izakji.agent.conversation.Conversation;
import com.izakji.agent.conversation.ConversationStore;
import com.izakji.agent.provider.ProviderRegistry;
import com.izakji.agent.skill.Skill;
import com.izakji.agent.util.Redactor;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
    private final List<Skill> skills;
    private final ExecutorService executor;
    private final Clock clock;
    private final Map<String, TurnHandle> active = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<ApprovalChoice>> pendingApprovals = new ConcurrentHashMap<>();
    private final SystemPromptBuilder promptBuilder = new SystemPromptBuilder();
    private final TokenEstimator estimator = new TokenEstimator();
    private final ConversationTruncator truncator = new ConversationTruncator(estimator);
    private final Redactor redactor;

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
        this(
                settings,
                providers,
                tools,
                conversations,
                permissions,
                contextProviders,
                agentsMarkdown,
                List.of(),
                List.of(),
                executor,
                clock);
    }

    public AgentRuntime(
            AgentSettings settings,
            ProviderRegistry providers,
            ToolRegistry tools,
            ConversationStore conversations,
            PermissionGate permissions,
            List<ContextProvider> contextProviders,
            String agentsMarkdown,
            List<Skill> skills,
            ExecutorService executor,
            Clock clock) {
        this(
                settings,
                providers,
                tools,
                conversations,
                permissions,
                contextProviders,
                agentsMarkdown,
                skills,
                List.of(),
                executor,
                clock);
    }

    public AgentRuntime(
            AgentSettings settings,
            ProviderRegistry providers,
            ToolRegistry tools,
            ConversationStore conversations,
            PermissionGate permissions,
            List<ContextProvider> contextProviders,
            String agentsMarkdown,
            List<Skill> skills,
            List<String> secrets,
            ExecutorService executor,
            Clock clock) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.providers = Objects.requireNonNull(providers, "providers");
        this.tools = Objects.requireNonNull(tools, "tools");
        this.conversations = Objects.requireNonNull(conversations, "conversations");
        this.permissions = Objects.requireNonNull(permissions, "permissions");
        this.contextProviders = List.copyOf(Objects.requireNonNullElse(contextProviders, List.of()));
        this.agentsMarkdown = agentsMarkdown == null ? "" : agentsMarkdown;
        this.skills = List.copyOf(Objects.requireNonNullElse(skills, List.of()));
        this.executor = Objects.requireNonNull(executor, "executor");
        this.clock = Objects.requireNonNull(clock, "clock");
        java.util.Set<String> allSecrets = new java.util.LinkedHashSet<>();
        settings.profiles().forEach(profile -> {
            if (!profile.apiKey().isBlank()) {
                allSecrets.add(profile.apiKey());
            }
            profile.headers().values().forEach(allSecrets::add);
        });
        Objects.requireNonNullElse(secrets, List.<String>of()).forEach(allSecrets::add);
        this.redactor = new Redactor(allSecrets);
    }

    public Conversation createConversation(String title, String profileId) {
        Conversation conversation = conversations.create(title, profileId);
        conversations.save(conversation);
        return conversation;
    }

    public TurnHandle startTurn(String conversationId, String userText, TurnObserver observer) {
        Conversation conversation = conversations
                .load(conversationId)
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
        CompletableFuture<ApprovalChoice> future = pendingApprovals.get(approvalKey(conversationId, approvalId));
        if (future != null) {
            future.complete(choice);
        }
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

    private void runTurn(Conversation loaded, String userText, TurnObserver observer, TurnHandle handle) {
        String conversationId = loaded.id();
        try {
            if (handle.isCancelled()) {
                finish(conversationId, handle, observer, TurnOutcome.CANCELLED, "cancelled");
                return;
            }
            Instant now = clock.instant();
            Conversation conversation = loaded.withMessage(new UserMessage(List.of(new TextPart(userText))), now);
            conversations.save(conversation);

            String profileId = conversation.profileId();
            ProviderProfile profile = settings.profiles().stream()
                    .filter(candidate -> candidate.id().equals(profileId))
                    .findFirst()
                    .orElseThrow(() -> new AgentException("Provider profile not found: " + profileId));
            LlmProvider provider = providers
                    .forType(profile.type())
                    .orElseThrow(() -> new AgentException("Provider not registered: " + profile.type()));

            String systemPrompt =
                    promptBuilder.build(settings, skills, agentsMarkdown, snapshots(conversationId, handle));
            int historyBudget = Math.max(
                    1,
                    profile.contextWindow()
                            - profile.maxOutputTokens()
                            - estimator.estimate(new SystemMessage(systemPrompt)));

            int rounds = 0;
            int calls = 0;
            boolean contextRetried = false;
            boolean truncationNotified = false;
            while (rounds < settings.maxToolRounds()) {
                if (handle.isCancelled()) {
                    finish(conversationId, handle, observer, TurnOutcome.CANCELLED, "cancelled");
                    return;
                }
                rounds++;
                ConversationTruncator.TruncationResult history =
                        truncator.truncateDetailed(conversation.messages(), historyBudget);
                if (history.truncated() && !truncationNotified) {
                    observer.onHistoryTruncated();
                    truncationNotified = true;
                }
                List<ChatMessage> requestMessages = new ArrayList<>();
                requestMessages.add(new SystemMessage(systemPrompt));
                requestMessages.addAll(history.messages());
                AssistantMessage assistant;
                try {
                    assistant = callProvider(provider, profile, requestMessages, observer, handle);
                } catch (RuntimeException failure) {
                    if (!contextRetried && isContextOverflow(failure)) {
                        contextRetried = true;
                        historyBudget = Math.max(1, historyBudget / 2);
                        continue;
                    }
                    throw failure;
                }
                if (handle.isCancelled()) {
                    finish(conversationId, handle, observer, TurnOutcome.CANCELLED, "cancelled");
                    return;
                }
                conversation = conversation.withMessage(assistant, clock.instant());
                conversations.save(conversation);

                if (assistant.toolCalls().isEmpty()) {
                    finish(conversationId, handle, observer, TurnOutcome.COMPLETED, "");
                    return;
                }
                if (calls + assistant.toolCalls().size() > settings.maxToolCalls()) {
                    for (ToolCall call : assistant.toolCalls()) {
                        ToolResult result = error(call, "Tool-call limit reached");
                        conversation = conversation.withMessage(toolMessage(result, call.name()), clock.instant());
                    }
                    conversations.save(conversation);
                    finish(conversationId, handle, observer, TurnOutcome.MAX_ITERATIONS, "tool-call limit");
                    return;
                }
                for (ToolCall call : assistant.toolCalls()) {
                    calls++;
                    ToolExecution execution = executeTool(conversation, call, observer, handle);
                    conversation = execution.conversation();
                }
            }
            finish(conversationId, handle, observer, TurnOutcome.MAX_ITERATIONS, "tool-round limit");
        } catch (Exception failure) {
            if (handle.isCancelled()) {
                finish(conversationId, handle, observer, TurnOutcome.CANCELLED, "cancelled");
                return;
            }
            String message = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
            String safeMessage = redactor.redact(message);
            observer.onError(new AgentException("Provider failed: " + safeMessage, failure));
            finish(conversationId, handle, observer, TurnOutcome.ERROR, safeMessage);
        }
    }

    private AssistantMessage callProvider(
            LlmProvider provider,
            ProviderProfile profile,
            List<ChatMessage> messages,
            TurnObserver observer,
            TurnHandle handle) {
        StringBuilder text = new StringBuilder();
        StringBuilder reasoning = new StringBuilder();
        List<ContentPart> reasoningParts = new ArrayList<>();
        AtomicReference<String> signature = new AtomicReference<>("");
        List<ToolCall> toolCalls = new ArrayList<>();
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
            public void onReasoningSignature(String value) {
                if (!reasoning.isEmpty() || !value.isBlank()) {
                    reasoningParts.add(new ReasoningPart(reasoning.toString(), value));
                    reasoning.setLength(0);
                }
                signature.set(value);
            }

            @Override
            public void onToolCall(ToolCall call) {
                toolCalls.add(call);
            }

            @Override
            public void onUsage(int promptTokens, int completionTokens) {}

            @Override
            public void onFinish(com.izakji.agent.api.FinishReason reason) {}
        };
        List<ToolSchema> schemas = profile.tools() && provider.supportsTools() ? tools.schemas() : List.of();
        CompletableFuture<Void> future = provider.stream(
                new ChatRequest(profile, messages, schemas, profile.maxOutputTokens()), listener, handle::isCancelled);
        Runnable cancel = () -> future.cancel(true);
        handle.onCancel(cancel);
        try {
            future.join();
        } finally {
            handle.removeOnCancel(cancel);
        }
        List<ContentPart> parts = new ArrayList<>();
        parts.addAll(reasoningParts);
        if (!reasoning.isEmpty()) {
            parts.add(new ReasoningPart(reasoning.toString(), signature.get()));
        }
        if (!text.isEmpty()) {
            parts.add(new TextPart(text.toString()));
        }
        return new AssistantMessage(parts, toolCalls);
    }

    private ToolExecution executeTool(
            Conversation conversation, ToolCall call, TurnObserver observer, TurnHandle handle) {
        AgentTool tool = tools.find(call.name()).orElse(null);
        if (tool == null) {
            return rejected(conversation, call, observer, "Unknown tool: " + call.name());
        }
        PermissionDecision decision = permissions.decide(
                tool, new com.izakji.agent.api.ToolInvocation(call.id(), call.name(), call.arguments()), conversation);
        if (decision.kind() == PermissionKind.DENY) {
            return rejected(conversation, call, observer, "Permission denied: " + decision.reason());
        }
        if (decision.kind() == PermissionKind.ASK) {
            ApprovalRequest request =
                    new ApprovalRequest(null, tool.name(), tool.risk(), call.arguments(), "Run " + tool.name());
            CompletableFuture<ApprovalChoice> future = new CompletableFuture<>();
            String key = approvalKey(conversation.id(), request.id());
            pendingApprovals.put(key, future);
            observer.onApprovalNeeded(request);
            ApprovalChoice choice;
            try {
                choice = future.get(settings.approvalTimeoutSeconds(), TimeUnit.SECONDS);
            } catch (TimeoutException timeout) {
                choice = ApprovalChoice.DENY;
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                choice = ApprovalChoice.DENY;
            } catch (ExecutionException failure) {
                choice = ApprovalChoice.DENY;
            } finally {
                pendingApprovals.remove(key);
            }
            if (choice == ApprovalChoice.DENY) {
                return rejected(conversation, call, observer, "Permission denied: user denied the tool call");
            }
            if (choice == ApprovalChoice.ALLOW_CONVERSATION && tool.allowsConversationApproval()) {
                conversation = conversation.withConversationApproval(
                        PermissionGate.approvalKey(tool.name(), call.arguments()), clock.instant());
                conversations.save(conversation);
            }
        }
        if (handle.isCancelled()) {
            return rejected(conversation, call, observer, "Cancelled");
        }
        observer.onToolStarted(call);
        ToolResult result;
        CompletableFuture<ToolResult> future = tool.execute(
                new com.izakji.agent.api.ToolInvocation(call.id(), call.name(), call.arguments()),
                new ToolContext(conversation.id(), handle::isCancelled));
        Runnable cancel = () -> future.cancel(true);
        handle.onCancel(cancel);
        try {
            result = future.get(60, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            result = error(call, "Tool interrupted");
        } catch (CancellationException cancelled) {
            result = error(call, "Cancelled");
        } catch (ExecutionException | TimeoutException failure) {
            future.cancel(true);
            result = error(call, "Tool failed: " + failure.getMessage());
        } finally {
            handle.removeOnCancel(cancel);
        }
        result = new ToolResult(
                result.callId(), redactor.redact(result.content()), result.error(), result.attachments());
        observer.onToolFinished(result);
        com.izakji.agent.api.ToolMessage message = toolMessage(result, call.name());
        Conversation updated = conversation.withMessage(message, clock.instant());
        conversations.save(updated);
        return new ToolExecution(updated, message);
    }

    private ToolExecution rejected(Conversation conversation, ToolCall call, TurnObserver observer, String message) {
        ToolResult result = error(call, message);
        observer.onToolFinished(result);
        com.izakji.agent.api.ToolMessage toolMessage = toolMessage(result, call.name());
        Conversation updated = conversation.withMessage(toolMessage, clock.instant());
        conversations.save(updated);
        return new ToolExecution(updated, toolMessage);
    }

    private static ToolResult error(ToolCall call, String message) {
        return new ToolResult(call.id(), message, true, List.of());
    }

    private static com.izakji.agent.api.ToolMessage toolMessage(ToolResult result, String name) {
        List<ContentPart> parts = new ArrayList<>();
        if (!result.content().isBlank()) {
            parts.add(new TextPart(result.content()));
        }
        parts.addAll(result.attachments());
        return new com.izakji.agent.api.ToolMessage(result.callId(), name, parts, result.error());
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
                    values.add(redactor.redact(value));
                }
            } catch (RuntimeException ignored) {
                // A failed optional context provider must not fail the turn.
            }
        }
        return values;
    }

    private void cancelTurn(String conversationId, TurnHandle handle) {
        pendingApprovals.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(conversationId + "\u0000"))
                .forEach(entry -> entry.getValue().complete(ApprovalChoice.DENY));
    }

    private void finish(
            String conversationId, TurnHandle handle, TurnObserver observer, TurnOutcome outcome, String error) {
        active.remove(conversationId, handle);
        TurnResult result = new TurnResult(outcome, conversationId, error);
        observer.onFinished(result);
        handle.complete(result);
    }

    private static boolean isContextOverflow(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            String message =
                    current.getMessage() == null ? "" : current.getMessage().toLowerCase(java.util.Locale.ROOT);
            if ((message.contains("context")
                            && (message.contains("length")
                                    || message.contains("window")
                                    || message.contains("maximum")
                                    || message.contains("too long")))
                    || message.contains("too many tokens")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String approvalKey(String conversationId, String approvalId) {
        return conversationId + "\u0000" + approvalId;
    }

    private record ToolExecution(Conversation conversation, com.izakji.agent.api.ToolMessage message) {}
}
