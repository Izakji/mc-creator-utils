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
    private final AgentChatModel model = new AgentChatModel();
    private AgentChatPresenter presenter = new AgentChatPresenter() {
        @Override
        public void requestFocus() {}

        @Override
        public void refresh() {}
    };
    private volatile boolean busy;
    private volatile String conversationId = "";
    private volatile String profileId = "";
    private volatile Conversation conversation;
    private volatile TurnHandle handle;

    public AgentChatSession(AgentTurnPort port, Executor uiExecutor) {
        this.port = port;
        this.uiExecutor = uiExecutor;
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
                    current = new AgentChatSession(AgentClientService.instance(), Minecraft.getInstance()::execute);
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

    public boolean busy() {
        return busy;
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
     * Opens an existing conversation in the chat window.
     *
     * @param id conversation id
     * @return true when loaded
     */
    public boolean open(String id) {
        Optional<Conversation> loaded = port.conversations().load(id);
        if (loaded.isEmpty()) {
            model.addError("Conversation could not be loaded.");
            presenter.refresh();
            return false;
        }
        conversation = loaded.get();
        conversationId = conversation.id();
        profileId = conversation.profileId();
        model.load(conversation);
        presenter.refresh();
        return true;
    }

    /**
     * Creates a new persisted conversation for a profile.
     *
     * @param selectedProfile profile id
     * @return true when created
     */
    public boolean newConversation(String selectedProfile) {
        if (!hasProfile(selectedProfile)) {
            model.addError("Choose a provider profile before starting a chat.");
            presenter.refresh();
            return false;
        }
        Conversation created = port.conversations().create("New Chat", selectedProfile);
        port.conversations().save(created);
        return open(created.id());
    }

    /**
     * Sends a user message and starts one turn.
     *
     * @param text user message
     * @return true when the turn started
     */
    public boolean send(String text) {
        if (busy || text == null || text.isBlank()) {
            return false;
        }
        if (!ensureConversation()) {
            return false;
        }
        boolean firstMessage = conversation.messages().isEmpty();
        model.addUser(text);
        if (firstMessage && "New Chat".equals(conversation.title())) {
            String title = com.izakji.mccreatorutils.agent.AgentChatTitles.fromFirstMessage(text);
            port.conversations().rename(conversationId, title);
            conversation = port.conversations().load(conversationId).orElse(conversation);
        }
        busy = true;
        presenter.refresh();
        try {
            handle = port.startTurn(conversationId, text, observer());
            return true;
        } catch (RuntimeException failure) {
            busy = false;
            model.addError(failure.getMessage() == null ? "Could not start the agent turn." : failure.getMessage());
            presenter.refresh();
            return false;
        }
    }

    public void stop() {
        TurnHandle current = handle;
        if (current != null) {
            current.cancel();
        }
        busy = false;
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
        stop();
        model.clear();
        conversationId = "";
        profileId = "";
        conversation = null;
        presenter.refresh();
    }

    private boolean ensureConversation() {
        if (!conversationId.isBlank() && conversation != null) {
            return hasProfile(profileId);
        }
        String selected = profileId;
        if (selected.isBlank()) {
            selected = port.settings().defaultProfileId();
        }
        if (!hasProfile(selected)) {
            model.addError("Add a provider profile in the Agent panel before chatting.");
            presenter.refresh();
            return false;
        }
        Conversation created = port.conversations().create("New Chat", selected);
        port.conversations().save(created);
        conversation = created;
        conversationId = created.id();
        profileId = selected;
        return true;
    }

    private boolean hasProfile(String id) {
        AgentSettings settings = port.settings();
        if (id == null || id.isBlank()) {
            return false;
        }
        return settings.profiles().stream().map(ProviderProfile::id).anyMatch(id::equals);
    }

    private TurnObserver observer() {
        return new TurnObserver() {
            @Override
            public void onTextDelta(String delta) {
                uiExecutor.execute(() -> {
                    model.appendAssistant(delta);
                    presenter.refresh();
                });
            }

            @Override
            public void onReasoningDelta(String delta) {
                uiExecutor.execute(() -> {
                    model.appendReasoning(delta);
                    presenter.refresh();
                });
            }

            @Override
            public void onToolStarted(ToolCall call) {
                uiExecutor.execute(() -> {
                    model.addToolStarted(call.name(), call.arguments());
                    presenter.refresh();
                });
            }

            @Override
            public void onToolFinished(ToolResult result) {
                uiExecutor.execute(() -> {
                    model.addToolFinished("Tool", result.error(), result.content());
                    model.clearApproval();
                    presenter.refresh();
                });
            }

            @Override
            public void onApprovalNeeded(ApprovalRequest request) {
                uiExecutor.execute(() -> {
                    model.addApproval(request);
                    presenter.requestFocus();
                    presenter.refresh();
                });
            }

            @Override
            public void onHistoryTruncated() {
                uiExecutor.execute(() -> {
                    model.addNotice("Earlier conversation history was truncated.");
                    presenter.refresh();
                });
            }

            @Override
            public void onError(AgentException error) {
                uiExecutor.execute(() -> {
                    model.addError(error.getMessage() == null ? "Agent error" : error.getMessage());
                    presenter.refresh();
                });
            }

            @Override
            public void onFinished(TurnResult result) {
                uiExecutor.execute(() -> {
                    busy = false;
                    if (result.outcome() == TurnOutcome.CANCELLED) {
                        model.addNotice("Turn cancelled.");
                    } else if (result.outcome() == TurnOutcome.ERROR
                            && !result.error().isBlank()) {
                        model.addError(result.error());
                    }
                    presenter.refresh();
                });
            }
        };
    }
}
