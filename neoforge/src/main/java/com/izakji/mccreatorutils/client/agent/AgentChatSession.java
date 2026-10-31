package com.izakji.mccreatorutils.client.agent;

import com.izakji.agent.api.AgentException;
import com.izakji.agent.api.ToolCall;
import com.izakji.agent.api.ToolResult;
import com.izakji.agent.config.AgentSettings;
import com.izakji.agent.config.ProviderProfile;
import com.izakji.agent.conversation.Conversation;
import com.izakji.agent.runtime.ApprovalChoice;
import com.izakji.agent.runtime.ApprovalRequest;
import com.izakji.agent.runtime.TurnHandle;
import com.izakji.agent.runtime.TurnObserver;
import com.izakji.agent.runtime.TurnOutcome;
import com.izakji.agent.runtime.TurnResult;
import com.izakji.mccreatorutils.agent.AgentChatTitles;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import net.minecraft.client.Minecraft;

/**
 * Connects one visible chat conversation to the runtime and its UI model.
 *
 * @author Izakji
 */
public final class AgentChatSession {
    private static volatile AgentChatSession instance;

    private final AgentTurnPort port;
    private final Executor uiExecutor;
    private final Executor ioExecutor;
    private final AgentChatModel model = new AgentChatModel();
    private AgentChatPresenter presenter = new AgentChatPresenter() {
        @Override
        public void requestFocus() {}

        @Override
        public void refresh() {}
    };
    private volatile boolean busy;
    private volatile boolean opening;
    private volatile String conversationId = "";
    private volatile String profileId = "";
    private volatile Conversation conversation;
    private volatile TurnHandle handle;
    private volatile long turnGeneration;

    public AgentChatSession(AgentTurnPort port, Executor uiExecutor) {
        this(port, uiExecutor, uiExecutor);
    }

    public AgentChatSession(AgentTurnPort port, Executor uiExecutor, Executor ioExecutor) {
        this.port = port;
        this.uiExecutor = uiExecutor;
        this.ioExecutor = ioExecutor;
    }

    /**
     * Returns the shared chat session backed by the client service.
     *
     * @return shared session
     */
    public static AgentChatSession instance() {
        AgentChatSession current = instance;
        if (current == null) {
            synchronized (AgentChatSession.class) {
                current = instance;
                if (current == null) {
                    AgentClientService service = AgentClientService.instance();
                    current = new AgentChatSession(service, Minecraft.getInstance()::execute, service.ioExecutor());
                    instance = current;
                }
            }
        }
        return current;
    }

    public void presenter(AgentChatPresenter presenter) {
        this.presenter = presenter == null
                ? new AgentChatPresenter() {
                    @Override
                    public void requestFocus() {}

                    @Override
                    public void refresh() {}
                }
                : presenter;
    }

    public AgentChatModel model() {
        return model;
    }

    public List<ProviderProfile> profiles() {
        return port.settings().profiles();
    }

    public String settingsDefaultProfile() {
        return port.settings().defaultProfileId();
    }

    public boolean busy() {
        return busy;
    }

    public boolean ready() {
        TurnHandle current = handle;
        return !busy && !opening && (current == null || current.completion().isDone());
    }

    public String conversationId() {
        return conversationId;
    }

    public String profileId() {
        return profileId;
    }

    public Optional<Conversation> conversation() {
        return Optional.ofNullable(conversation);
    }

    /**
     * Opens an existing conversation in the chat window. File work runs off the UI thread.
     *
     * @param id conversation id
     * @return true when the open was scheduled
     */
    public boolean open(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        cancelActiveTurn();
        long generation = ++turnGeneration;
        opening = true;
        model.clear();
        presenter.refresh();
        ioExecutor.execute(() -> {
            Optional<Conversation> loaded;
            try {
                loaded = port.conversations().load(id);
            } catch (RuntimeException failure) {
                uiExecutor.execute(() -> failOpen(generation, "Conversation could not be loaded."));
                return;
            }
            uiExecutor.execute(() -> {
                if (generation != turnGeneration) {
                    return;
                }
                opening = false;
                if (loaded.isEmpty()) {
                    model.addError("Conversation could not be loaded.");
                } else {
                    applyConversation(loaded.get());
                }
                presenter.refresh();
            });
        });
        return true;
    }

    /**
     * Creates a new persisted conversation for a profile. File work runs off the UI thread.
     *
     * @param selectedProfile profile id
     * @return true when creation was scheduled
     */
    public boolean newConversation(String selectedProfile) {
        if (!hasProfile(selectedProfile)) {
            model.addError("Choose a provider profile before starting a chat.");
            presenter.refresh();
            return false;
        }
        cancelActiveTurn();
        long generation = ++turnGeneration;
        opening = true;
        model.clear();
        presenter.refresh();
        ioExecutor.execute(() -> {
            try {
                Conversation created = port.conversations().create("New Chat", selectedProfile);
                port.conversations().save(created);
                uiExecutor.execute(() -> {
                    if (generation != turnGeneration) {
                        return;
                    }
                    opening = false;
                    applyConversation(created);
                    presenter.refresh();
                });
            } catch (RuntimeException failure) {
                uiExecutor.execute(() -> failOpen(generation, "Could not create the conversation."));
            }
        });
        return true;
    }

    /**
     * Sends a user message and starts one turn. Conversation file work runs off the UI thread.
     *
     * @param text user message
     * @return true when the send was accepted
     */
    public boolean send(String text) {
        if (busy || opening || text == null || text.isBlank()) {
            return false;
        }
        TurnHandle current = handle;
        if (current != null && !current.completion().isDone()) {
            model.addError("Previous turn is still stopping; try again in a moment.");
            presenter.refresh();
            return false;
        }
        String selected = conversation != null ? conversation.profileId() : profileId;
        if (conversation != null && !hasProfile(selected)) {
            model.addError("This conversation's provider profile was deleted. Choose a profile or start a new chat.");
            presenter.refresh();
            return false;
        }
        if (selected.isBlank()) {
            selected = port.settings().defaultProfileId();
        }
        if (!hasProfile(selected)) {
            model.addError("Add a provider profile in the Agent panel before chatting.");
            presenter.refresh();
            return false;
        }
        ProviderProfile profile = profile(selected).orElseThrow();
        if (!hasCredential(profile)) {
            model.addError("Add an API key in the Agent panel before sending.");
            presenter.refresh();
            return false;
        }

        String selectedProfile = selected;
        long generation = ++turnGeneration;
        Conversation existing = conversation;
        boolean firstMessage = existing == null || existing.messages().isEmpty();
        model.addUser(text);
        busy = true;
        presenter.refresh();
        ioExecutor.execute(() -> {
            try {
                Conversation target = existing;
                if (target == null) {
                    target = port.conversations().create("New Chat", selectedProfile);
                    port.conversations().save(target);
                }
                if (firstMessage && "New Chat".equals(target.title())) {
                    target = target.withTitle(AgentChatTitles.fromFirstMessage(text), Instant.now());
                    port.conversations().save(target);
                }
                Conversation ready = target;
                if (generation != turnGeneration) {
                    return;
                }
                TurnHandle started;
                try {
                    started = port.startTurn(ready.id(), text, observer(generation));
                } catch (RuntimeException failure) {
                    uiExecutor.execute(() -> failTurn(generation, friendlyStartError(failure)));
                    return;
                }
                uiExecutor.execute(() -> {
                    if (generation != turnGeneration) {
                        started.cancel();
                        return;
                    }
                    conversation = ready;
                    conversationId = ready.id();
                    profileId = ready.profileId();
                    handle = started;
                    busy = true;
                    presenter.refresh();
                });
            } catch (RuntimeException failure) {
                uiExecutor.execute(() -> failTurn(generation, "Could not prepare the conversation."));
            }
        });
        return true;
    }

    public void stop() {
        turnGeneration++;
        TurnHandle current = handle;
        if (current != null) {
            current.cancel();
            current.completion()
                    .whenComplete((result, failure) -> uiExecutor.execute(() -> {
                        if (handle == current) {
                            handle = null;
                        }
                    }));
        }
        busy = false;
        opening = false;
        model.clearApproval();
        presenter.refresh();
    }

    public void approve(String approvalId, ApprovalChoice choice) {
        if (!conversationId.isBlank()) {
            port.approve(conversationId, approvalId, choice);
        }
        model.clearApproval();
        presenter.refresh();
    }

    public void onLogout() {
        turnGeneration++;
        TurnHandle current = handle;
        if (current != null) {
            current.cancel();
        }
        handle = null;
        busy = false;
        opening = false;
        model.clear();
        model.clearApproval();
        conversationId = "";
        profileId = "";
        conversation = null;
        presenter.refresh();
    }

    private void cancelActiveTurn() {
        turnGeneration++;
        TurnHandle current = handle;
        if (current != null) {
            current.cancel();
        }
        handle = null;
        busy = false;
        opening = false;
        model.clearApproval();
    }

    private void applyConversation(Conversation loaded) {
        conversation = loaded;
        conversationId = loaded.id();
        profileId = loaded.profileId();
        model.load(loaded);
    }

    private void failOpen(long generation, String message) {
        if (generation != turnGeneration) {
            return;
        }
        opening = false;
        model.addError(message);
        presenter.refresh();
    }

    private void failTurn(long generation, String message) {
        if (generation != turnGeneration) {
            return;
        }
        busy = false;
        model.addError(message);
        presenter.refresh();
    }

    private boolean hasProfile(String id) {
        return profile(id).isPresent();
    }

    private Optional<ProviderProfile> profile(String id) {
        AgentSettings settings = port.settings();
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return settings.profiles().stream()
                .filter(profile -> profile.id().equals(id))
                .findFirst();
    }

    private static boolean hasCredential(ProviderProfile profile) {
        if (!profile.apiKey().isBlank()) {
            return true;
        }
        try {
            String host = URI.create(profile.baseUrl()).getHost();
            return host != null
                    && (host.equalsIgnoreCase("localhost")
                            || host.equals("127.0.0.1")
                            || host.equals("::1")
                            || host.equals("0.0.0.0"));
        } catch (IllegalArgumentException malformed) {
            return false;
        }
    }

    private static String friendlyStartError(RuntimeException failure) {
        String message = failure.getMessage() == null ? "Could not start the agent turn." : failure.getMessage();
        if (message.contains("already running")) {
            return "Previous turn is still stopping; try again in a moment.";
        }
        return message;
    }

    private TurnObserver observer(long generation) {
        return new TurnObserver() {
            private boolean stale() {
                return generation != turnGeneration;
            }

            @Override
            public void onTextDelta(String delta) {
                uiExecutor.execute(() -> {
                    if (stale()) {
                        return;
                    }
                    model.appendAssistant(delta);
                    presenter.refresh();
                });
            }

            @Override
            public void onReasoningDelta(String delta) {
                uiExecutor.execute(() -> {
                    if (stale()) {
                        return;
                    }
                    model.appendReasoning(delta);
                    presenter.refresh();
                });
            }

            @Override
            public void onToolStarted(ToolCall call) {
                uiExecutor.execute(() -> {
                    if (stale()) {
                        return;
                    }
                    model.addToolStarted(call.name(), call.arguments());
                    presenter.refresh();
                });
            }

            @Override
            public void onToolFinished(ToolResult result) {
                uiExecutor.execute(() -> {
                    if (stale()) {
                        return;
                    }
                    model.addToolFinished("Tool", result.error(), result.content());
                    model.clearApproval();
                    presenter.refresh();
                });
            }

            @Override
            public void onApprovalNeeded(ApprovalRequest request) {
                uiExecutor.execute(() -> {
                    if (stale()) {
                        return;
                    }
                    model.addApproval(request);
                    presenter.requestFocus();
                    presenter.refresh();
                });
            }

            @Override
            public void onHistoryTruncated() {
                uiExecutor.execute(() -> {
                    if (stale()) {
                        return;
                    }
                    model.addNotice("Earlier conversation history was truncated.");
                    presenter.refresh();
                });
            }

            @Override
            public void onError(AgentException error) {
                uiExecutor.execute(() -> {
                    if (stale()) {
                        return;
                    }
                    model.addError(error.getMessage() == null ? "Agent error" : error.getMessage());
                    presenter.refresh();
                });
            }

            @Override
            public void onFinished(TurnResult result) {
                uiExecutor.execute(() -> {
                    if (stale()) {
                        return;
                    }
                    busy = false;
                    handle = null;
                    if (result.outcome() == TurnOutcome.CANCELLED) {
                        model.addNotice("Turn cancelled.");
                    } else if (result.outcome() == TurnOutcome.MAX_ITERATIONS) {
                        model.addNotice("Tool limit reached.");
                    }
                    presenter.refresh();
                });
            }
        };
    }
}
