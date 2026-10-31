package com.izakji.mccreatorutils.client.agent;

import com.izakji.agent.config.ProviderProfile;
import com.izakji.agent.runtime.ApprovalChoice;
import com.izakji.agent.runtime.ApprovalRequest;
import com.izakji.mccreatorutils.agent.AgentChatGeometry;
import com.izakji.mccreatorutils.agent.AgentChatLayout;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Focused, draggable agent chat window.
 *
 * @author Izakji
 */
public final class AgentChatScreen extends Screen {
    private final AgentChatSession session = AgentChatSession.instance();
    private final AgentChatWindow chatWindow = AgentChatWindow.instance();
    private final AgentChatScreenState screenState = new AgentChatScreenState();
    private EditBox input;
    private Button sendButton;
    private Button stopButton;
    private Button newButton;
    private Button profileButton;
    private Button closeButton;
    private Button fullscreenButton;
    private Button approveOnceButton;
    private Button allowConversationButton;
    private Button denyButton;
    private AgentChatLayout layout;
    private List<AgentChatRenderer.EntryHit> hits = List.of();
    private String selectedProfile = "";
    private boolean dragging;
    private boolean resizing;
    private int dragOffsetX;
    private int dragOffsetY;
    private int resizeStartWidth;
    private int resizeStartHeight;
    private double resizeStartX;
    private double resizeStartY;
    private long lastRevision = -1;

    public AgentChatScreen() {
        super(Component.translatable("screen.mccreatorutils.agent_chat"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        chatWindow.show();
        selectedProfile = session.profileId().isBlank() ? session.settingsDefaultProfile() : session.profileId();
        layout = chatWindow.state().layout(width, height);

        input = new EditBox(
                font,
                inputBounds().x(),
                inputBounds().y(),
                Math.max(1, inputBounds().width()),
                Math.max(1, inputBounds().height()),
                Component.translatable("screen.mccreatorutils.agent_chat.input"));
        input.setMaxLength(4096);
        input.setHint(Component.translatable("screen.mccreatorutils.agent_chat.input"));
        addRenderableWidget(input);

        sendButton = addButton("send", this::send);
        stopButton = addButton("stop", session::stop);
        newButton = addButton("new", this::newChat);
        profileButton = addButton("profile", this::cycleProfile);
        closeButton = addButton("close", this::onClose);
        fullscreenButton = addButton("fullscreen", this::toggleFullscreen);
        approveOnceButton = addApprovalButton("approve_once", ApprovalChoice.APPROVE_ONCE);
        allowConversationButton = addApprovalButton("allow_conversation", ApprovalChoice.ALLOW_CONVERSATION);
        denyButton = addApprovalButton("deny", ApprovalChoice.DENY);

        session.presenter(new AgentChatPresenter() {
            @Override
            public void requestFocus() {
                if (minecraft != null && minecraft.screen != AgentChatScreen.this) {
                    AgentChatWindow.instance().focusOrOpen();
                } else if (input != null) {
                    setFocused(input);
                    input.setFocused(true);
                }
            }

            @Override
            public void refresh() {
                updateWidgets();
            }
        });
        updateWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        layout = chatWindow.state().layout(width, height);
        hits = AgentChatRenderer.renderWithHits(graphics, session, chatWindow.state(), screenState, false);
        updateWidgets();
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Keep the game world visible behind the floating chat.
    }

    @Override
    public void tick() {
        super.tick();
        if (layout == null) {
            layout = chatWindow.state().layout(width, height);
        }
        if (session.model().revision() != lastRevision) {
            lastRevision = session.model().revision();
            updateWidgets();
        }
    }

    private void updateWidgets() {
        if (layout == null || input == null) {
            return;
        }
        layout = chatWindow.state().layout(width, height);
        AgentChatGeometry inputArea = inputBounds();
        input.setX(inputArea.x());
        input.setY(inputArea.y());
        input.setWidth(Math.max(1, inputArea.width()));
        input.setHeight(Math.max(1, inputArea.height()));

        sendButton.setX(layout.sendButton().x());
        sendButton.setY(layout.sendButton().y());
        sendButton.setWidth(layout.sendButton().width());
        sendButton.setHeight(layout.sendButton().height());
        stopButton.setX(layout.stopButton().x());
        stopButton.setY(layout.stopButton().y());
        stopButton.setWidth(layout.stopButton().width());
        stopButton.setHeight(layout.stopButton().height());
        newButton.setX(layout.newButton().x());
        newButton.setY(layout.newButton().y());
        newButton.setWidth(layout.newButton().width());
        newButton.setHeight(layout.newButton().height());
        profileButton.setX(layout.profileButton().x());
        profileButton.setY(layout.profileButton().y());
        profileButton.setWidth(layout.profileButton().width());
        profileButton.setHeight(layout.profileButton().height());
        closeButton.setX(layout.closeButton().x());
        closeButton.setY(layout.closeButton().y());
        closeButton.setWidth(layout.closeButton().width());
        closeButton.setHeight(layout.closeButton().height());
        fullscreenButton.setX(layout.fullscreenButton().x());
        fullscreenButton.setY(layout.fullscreenButton().y());
        fullscreenButton.setWidth(layout.fullscreenButton().width());
        fullscreenButton.setHeight(layout.fullscreenButton().height());

        profileButton.setMessage(
                Component.translatable("screen.mccreatorutils.agent_chat.profile", profileName(selectedProfile)));
        sendButton.active = !session.busy();
        stopButton.active = session.busy();
        newButton.active = !session.busy();
        profileButton.active = !session.busy();
        closeButton.setMessage(Component.literal("X"));
        fullscreenButton.setMessage(Component.literal(chatWindow.state().fullscreen() ? "-" : "+"));

        ApprovalRequest approval = session.model().pendingApproval().orElse(null);
        boolean approvalVisible = approval != null;
        input.visible = !approvalVisible;
        sendButton.visible = !approvalVisible;
        stopButton.visible = !approvalVisible;
        newButton.visible = !approvalVisible;
        profileButton.visible = !approvalVisible;
        approveOnceButton.visible = approvalVisible;
        allowConversationButton.visible = approvalVisible;
        denyButton.visible = approvalVisible;
        if (approvalVisible) {
            int widthEach = Math.max(1, layout.inputBar().width() / 3);
            placeApprovalButton(approveOnceButton, 0, widthEach);
            placeApprovalButton(allowConversationButton, 1, widthEach);
            placeApprovalButton(denyButton, 2, widthEach);
        }
    }

    private void placeApprovalButton(Button button, int index, int widthEach) {
        button.setX(layout.inputBar().x() + index * widthEach);
        button.setY(layout.inputBar().y() + 3);
        button.setWidth(widthEach - 4);
        button.setHeight(Math.max(1, layout.inputBar().height() - 6));
    }

    private AgentChatGeometry inputBounds() {
        AgentChatGeometry newChat = layout.newButton();
        AgentChatGeometry stop = layout.stopButton();
        int x = newChat.right() + 4;
        return new AgentChatGeometry(
                x,
                layout.inputBar().y() + 3,
                Math.max(1, stop.x() - x - 4),
                Math.max(1, layout.inputBar().height() - 6));
    }

    private Button addButton(String key, Runnable action) {
        return addRenderableWidget(Button.builder(
                        Component.translatable("screen.mccreatorutils.agent_chat." + key), ignored -> action.run())
                .bounds(0, 0, 1, 1)
                .build());
    }

    private Button addApprovalButton(String key, ApprovalChoice choice) {
        Button button = addRenderableWidget(Button.builder(
                        Component.translatable("screen.mccreatorutils.agent_chat." + key), ignored -> approve(choice))
                .bounds(0, 0, 1, 1)
                .build());
        button.visible = false;
        return button;
    }

    private void approve(ApprovalChoice choice) {
        session.model().pendingApproval().ifPresent(request -> session.approve(request.id(), choice));
    }

    private void send() {
        String text = input.getValue().strip();
        if (text.isEmpty() || session.busy()) {
            return;
        }
        if (session.send(text)) {
            input.setValue("");
        }
        input.setFocused(true);
    }

    private void newChat() {
        session.newConversation(selectedProfile);
        input.setValue("");
    }

    private void cycleProfile() {
        List<ProviderProfile> profiles = session.profiles();
        if (profiles.isEmpty()) {
            return;
        }
        int current = 0;
        for (int index = 0; index < profiles.size(); index++) {
            if (profiles.get(index).id().equals(selectedProfile)) {
                current = index;
                break;
            }
        }
        selectedProfile = profiles.get((current + 1) % profiles.size()).id();
        updateWidgets();
    }

    private String profileName(String id) {
        return session.profiles().stream()
                .filter(profile -> profile.id().equals(id))
                .map(ProviderProfile::displayName)
                .findFirst()
                .orElse("Provider");
    }

    private void toggleFullscreen() {
        chatWindow.state().toggleFullscreen(width, height);
        layout = chatWindow.state().layout(width, height);
        updateWidgets();
    }

    private boolean in(AgentChatGeometry rectangle, double mouseX, double mouseY) {
        return mouseX >= rectangle.x()
                && mouseY >= rectangle.y()
                && mouseX < rectangle.right()
                && mouseY < rectangle.bottom();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (layout == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (!in(layout.window(), mouseX, mouseY)) {
            onClose();
            return true;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (in(layout.resizeGrip(), mouseX, mouseY) && !chatWindow.state().fullscreen()) {
            resizing = true;
            resizeStartWidth = layout.window().width();
            resizeStartHeight = layout.window().height();
            resizeStartX = mouseX;
            resizeStartY = mouseY;
            return true;
        }
        if (in(layout.titleBar(), mouseX, mouseY)
                && !in(layout.closeButton(), mouseX, mouseY)
                && !in(layout.fullscreenButton(), mouseX, mouseY)
                && !chatWindow.state().fullscreen()) {
            dragging = true;
            dragOffsetX = (int) mouseX - layout.window().x();
            dragOffsetY = (int) mouseY - layout.window().y();
            return true;
        }
        for (AgentChatRenderer.EntryHit hit : hits) {
            if (in(hit.bounds(), mouseX, mouseY)) {
                screenState.toggleCollapsed(hit.entryIndex());
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        int width = this.width;
        int height = this.height;
        if (dragging) {
            chatWindow
                    .state()
                    .setBounds(
                            new AgentChatGeometry(
                                    (int) mouseX - dragOffsetX,
                                    (int) mouseY - dragOffsetY,
                                    layout.window().width(),
                                    layout.window().height()),
                            width,
                            height);
            updateWidgets();
            return true;
        }
        if (resizing) {
            chatWindow
                    .state()
                    .setBounds(
                            new AgentChatGeometry(
                                    layout.window().x(),
                                    layout.window().y(),
                                    resizeStartWidth + (int) (mouseX - resizeStartX),
                                    resizeStartHeight + (int) (mouseY - resizeStartY)),
                            width,
                            height);
            updateWidgets();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        resizing = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (layout != null && in(layout.transcript(), mouseX, mouseY)) {
            screenState.scrollBy((int) (scrollY * 12));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (chatWindow.state().fullscreen()) {
                toggleFullscreen();
            } else {
                onClose();
            }
            return true;
        }
        if (input != null
                && input.isFocused()
                && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            send();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (chatWindow.state().fullscreen()) {
            toggleFullscreen();
            return;
        }
        super.onClose();
    }

    @Override
    public void removed() {
        session.presenter(chatWindow);
        super.removed();
    }
}
