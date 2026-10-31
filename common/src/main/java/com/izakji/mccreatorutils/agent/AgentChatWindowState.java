package com.izakji.mccreatorutils.agent;

/**
 * Visibility, bounds, and fullscreen state for the floating agent chat.
 *
 * @author Izakji
 */
public final class AgentChatWindowState {
    private AgentChatGeometry bounds;
    private AgentChatGeometry restoreBounds;
    private boolean fullscreen;
    private boolean visible;

    public boolean visible() {
        return visible;
    }

    public boolean fullscreen() {
        return fullscreen;
    }

    /**
     * Shows the window, creating or clamping its bounds to the screen.
     *
     * @param screenWidth screen width
     * @param screenHeight screen height
     */
    public void show(int screenWidth, int screenHeight) {
        visible = true;
        bounds = bounds(screenWidth, screenHeight);
    }

    public void hide() {
        visible = false;
    }

    /**
     * Returns current bounds, creating the default centered window when absent.
     *
     * @param screenWidth screen width
     * @param screenHeight screen height
     * @return clamped bounds
     */
    public AgentChatGeometry bounds(int screenWidth, int screenHeight) {
        if (bounds == null) {
            bounds = defaultBounds(screenWidth, screenHeight);
        }
        bounds = bounds.clampTo(
                screenWidth, screenHeight, AgentChatLayout.MIN_WINDOW_WIDTH, AgentChatLayout.MIN_WINDOW_HEIGHT);
        return bounds;
    }

    /**
     * Replaces bounds with a clamped window.
     *
     * @param requested requested bounds
     * @param screenWidth screen width
     * @param screenHeight screen height
     */
    public void setBounds(AgentChatGeometry requested, int screenWidth, int screenHeight) {
        bounds = requested.clampTo(
                screenWidth, screenHeight, AgentChatLayout.MIN_WINDOW_WIDTH, AgentChatLayout.MIN_WINDOW_HEIGHT);
    }

    /**
     * Returns the layout for the current window.
     *
     * @param screenWidth screen width
     * @param screenHeight screen height
     * @return layout
     */
    public AgentChatLayout layout(int screenWidth, int screenHeight) {
        return AgentChatLayout.of(bounds(screenWidth, screenHeight));
    }

    /**
     * Toggles fullscreen and restores the previous bounds when leaving it.
     *
     * @param screenWidth screen width
     * @param screenHeight screen height
     */
    public void toggleFullscreen(int screenWidth, int screenHeight) {
        bounds = bounds(screenWidth, screenHeight);
        if (fullscreen) {
            bounds = (restoreBounds == null ? defaultBounds(screenWidth, screenHeight) : restoreBounds)
                    .clampTo(
                            screenWidth,
                            screenHeight,
                            AgentChatLayout.MIN_WINDOW_WIDTH,
                            AgentChatLayout.MIN_WINDOW_HEIGHT);
            restoreBounds = null;
            fullscreen = false;
        } else {
            restoreBounds = bounds;
            bounds = AgentChatGeometry.fullscreen(screenWidth, screenHeight, AgentChatLayout.FULLSCREEN_MARGIN);
            fullscreen = true;
        }
    }

    private static AgentChatGeometry defaultBounds(int screenWidth, int screenHeight) {
        int width = Math.max(
                AgentChatLayout.DEFAULT_WINDOW_WIDTH, screenWidth * AgentChatLayout.DEFAULT_SCREEN_PERCENT / 100);
        int height = Math.max(
                AgentChatLayout.DEFAULT_WINDOW_HEIGHT, screenHeight * AgentChatLayout.DEFAULT_SCREEN_PERCENT / 100);
        return AgentChatGeometry.centered(screenWidth, screenHeight, width, height);
    }
}
