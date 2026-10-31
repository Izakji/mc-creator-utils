package com.izakji.mccreatorutils.client.agent;

import com.izakji.mccreatorutils.McCreatorUtils;
import com.izakji.mccreatorutils.agent.AgentChatWindowState;
import net.minecraft.client.Minecraft;

/**
 * UI-side owner of the floating agent chat window.
 *
 * @author Izakji
 */
public final class AgentChatWindow implements AgentChatPresenter {
    private static final AgentChatWindow INSTANCE = new AgentChatWindow();

    private final AgentChatWindowState state = new AgentChatWindowState();

    private AgentChatWindow() {}

    public static AgentChatWindow instance() {
        return INSTANCE;
    }

    public AgentChatWindowState state() {
        return state;
    }

    public boolean visible() {
        return state.visible();
    }

    public void show() {
        Minecraft minecraft = Minecraft.getInstance();
        state.show(
                minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
    }

    public void hide() {
        state.hide();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof AgentChatScreen) {
            minecraft.setScreen(null);
        }
    }

    /**
     * Shows and focuses the chat, leaving any other screen alone.
     */
    public void focusOrOpen() {
        AgentChatSession session;
        try {
            session = AgentChatSession.instance();
        } catch (RuntimeException failure) {
            McCreatorUtils.LOGGER.error("Could not open Agent Chat", failure);
            return;
        }
        show();
        session.presenter(this);
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof AgentChatScreen)) {
            minecraft.setScreen(new AgentChatScreen());
        }
    }

    @Override
    public void requestFocus() {
        focusOrOpen();
    }

    @Override
    public void refresh() {}
}
