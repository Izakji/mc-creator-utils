package com.izakji.mccreatorutils.client.agent;

import com.izakji.agent.conversation.ConversationSummary;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import dev.vfyjxf.taffy.style.FlexDirection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Conversation list and actions in the Agent panel.
 *
 * @author Izakji
 */
public final class AgentChatTab extends UIElement {
    private final AgentChatSession session = AgentChatSession.instance();
    private final Selector<ConversationSummary> conversations = new Selector<>();
    private final TextField renameField = new TextField();
    private final Button renameButton = new Button();
    private final Button deleteButton = new Button();
    private final Label status = new Label();

    public AgentChatTab() {
        this.layout(layout -> layout.widthPercent(100)
                .heightPercent(100)
                .flexDirection(FlexDirection.COLUMN)
                .gapAll(5));

        var description = new Label();
        description.setText(Component.translatable("tool.mccreatorutils.agent.description"));
        addChild(description);

        conversations.layout(layout -> layout.widthPercent(100).height(16));
        conversations.setCandidateUIProvider(
                UIElementProvider.text(summary -> summary == null ? Component.empty() : candidateLabel(summary)));
        conversations.setOnValueChanged(summary -> {
            if (summary != null) {
                renameField.setText(summary.title());
                deleteButton.setActive(true);
            }
        });
        addChild(conversations);

        var renameRow = new UIElement();
        renameRow.layout(layout -> layout.widthPercent(100)
                .height(16)
                .flexDirection(FlexDirection.ROW)
                .gapAll(4));
        renameField.layout(layout -> layout.flex(1).height(16));
        renameField.textFieldStyle(
                style -> style.placeholder(Component.translatable("tool.mccreatorutils.agent.rename")));
        renameButton.setText(Component.translatable("tool.mccreatorutils.agent.rename"));
        renameButton.layout(layout -> layout.width(64).height(16));
        renameButton.setOnClick(event -> renameSelected());
        renameRow.addChildren(renameField, renameButton);
        addChild(renameRow);

        var buttonRow = new UIElement();
        buttonRow.layout(layout -> layout.widthPercent(100)
                .height(16)
                .flexDirection(FlexDirection.ROW)
                .gapAll(4));
        var newButton = new Button();
        newButton.setText(Component.translatable("tool.mccreatorutils.agent.new_chat"));
        newButton.layout(layout -> layout.flex(1).height(16));
        newButton.setOnClick(event -> newChat());
        deleteButton.setText(Component.translatable("tool.mccreatorutils.agent.delete"));
        deleteButton.layout(layout -> layout.width(64).height(16));
        deleteButton.setActive(false);
        deleteButton.setOnClick(event -> deleteSelected());
        var openButton = new Button();
        openButton.setText(Component.translatable("tool.mccreatorutils.agent.open_chat"));
        openButton.layout(layout -> layout.flex(1).height(16));
        openButton.setOnClick(event -> openSelected());
        buttonRow.addChildren(newButton, deleteButton, openButton);
        addChild(buttonRow);

        status.layout(layout -> layout.widthPercent(100));
        addChild(status);

        refresh();
    }

    private void refresh() {
        ConversationSummary selected = conversations.getValue();
        String selectedId = selected == null ? "" : selected.id();
        status.setText(Component.literal("Loading conversations..."));
        CompletableFuture.supplyAsync(
                        () -> AgentClientService.instance().listConversations(),
                        AgentClientService.instance().ioExecutor())
                .whenComplete((loaded, failure) -> Minecraft.getInstance().execute(() -> {
                    if (failure != null) {
                        status.setText(Component.literal("Could not load conversations."));
                        status.textStyle(style -> style.textColor(ColorPattern.RED.color));
                        return;
                    }
                    applyConversations(loaded, selectedId);
                }));
    }

    private void applyConversations(List<ConversationSummary> loaded, String selectedId) {
        conversations.setCandidates(loaded);
        if (!selectedId.isBlank()) {
            loaded.stream()
                    .filter(summary -> summary.id().equals(selectedId))
                    .findFirst()
                    .ifPresent(conversations::setSelected);
        } else if (!loaded.isEmpty()) {
            conversations.setSelected(loaded.get(0));
        }
        ConversationSummary current = conversations.getValue();
        if (current == null) {
            renameField.setText("");
            deleteButton.setActive(false);
            String warning = AgentClientService.instance().startupWarning();
            status.setText(
                    warning.isBlank()
                            ? Component.translatable("tool.mccreatorutils.agent.no_conversations")
                            : Component.literal(warning));
            if (!warning.isBlank()) {
                status.textStyle(style -> style.textColor(ColorPattern.YELLOW.color));
            }
        } else {
            renameField.setText(current.title());
            deleteButton.setActive(session.ready());
            status.setText(Component.literal(""));
        }
    }

    private boolean readyForAction() {
        if (session.ready()) {
            return true;
        }
        status.setText(Component.literal("Wait for the current agent turn to finish."));
        status.textStyle(style -> style.textColor(ColorPattern.RED.color));
        return false;
    }

    private void newChat() {
        if (!readyForAction()) {
            return;
        }
        String profile = session.profileId();
        if (profile.isBlank()) {
            profile = session.settingsDefaultProfile();
        }
        if (profile.isBlank()) {
            status.setText(Component.translatable("tool.mccreatorutils.agent.no_provider"));
            status.textStyle(style -> style.textColor(ColorPattern.RED.color));
            return;
        }
        if (session.newConversation(profile)) {
            AgentChatWindow.instance().focusOrOpen();
        } else {
            status.setText(Component.literal("Could not create the conversation."));
            status.textStyle(style -> style.textColor(ColorPattern.RED.color));
        }
    }

    private void renameSelected() {
        if (!readyForAction()) {
            return;
        }
        ConversationSummary selected = conversations.getValue();
        if (selected == null) {
            return;
        }
        String title = renameField.getRawText().strip();
        if (title.isEmpty()) {
            status.setText(Component.translatable("tool.mccreatorutils.agent.rename"));
            status.textStyle(style -> style.textColor(ColorPattern.RED.color));
            return;
        }
        String id = selected.id();
        CompletableFuture.runAsync(
                        () -> AgentClientService.instance().renameConversation(id, title),
                        AgentClientService.instance().ioExecutor())
                .whenComplete((ignored, failure) -> Minecraft.getInstance().execute(() -> {
                    if (failure != null) {
                        status.setText(Component.literal("Could not rename the conversation."));
                        status.textStyle(style -> style.textColor(ColorPattern.RED.color));
                    } else {
                        refresh();
                    }
                }));
    }

    private void deleteSelected() {
        if (!readyForAction()) {
            return;
        }
        ConversationSummary selected = conversations.getValue();
        if (selected == null) {
            return;
        }
        String id = selected.id();
        CompletableFuture.runAsync(
                        () -> AgentClientService.instance().deleteConversation(id),
                        AgentClientService.instance().ioExecutor())
                .whenComplete((ignored, failure) -> Minecraft.getInstance().execute(() -> {
                    if (failure != null) {
                        status.setText(Component.literal("Could not delete the conversation."));
                        status.textStyle(style -> style.textColor(ColorPattern.RED.color));
                        return;
                    }
                    if (id.equals(session.conversationId())) {
                        session.onLogout();
                    }
                    refresh();
                }));
    }

    private void openSelected() {
        if (!readyForAction()) {
            return;
        }
        ConversationSummary selected = conversations.getValue();
        if (selected == null) {
            return;
        }
        if (session.open(selected.id())) {
            AgentChatWindow.instance().focusOrOpen();
        } else {
            status.setText(Component.literal("Could not open the conversation."));
            status.textStyle(style -> style.textColor(ColorPattern.RED.color));
        }
    }

    private static Component candidateLabel(ConversationSummary summary) {
        String updated = summary.updated().toString().replace('T', ' ');
        if (updated.length() > 16) {
            updated = updated.substring(0, 16);
        }
        return Component.literal(summary.title() + "  ·  " + updated);
    }
}
