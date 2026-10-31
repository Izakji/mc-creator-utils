package com.izakji.mccreatorutils.agent;

/**
 * Layout rectangles for the agent chat window in GUI coordinates.
 *
 * @param window full window
 * @param titleBar draggable title bar
 * @param transcript scrolling transcript area
 * @param inputBar input row
 * @param profileButton profile selector button
 * @param newButton new-chat button
 * @param sendButton send button
 * @param stopButton stop button
 * @param fullscreenButton fullscreen/restore button
 * @param closeButton close button
 * @param resizeGrip bottom-right resize handle
 * @author Izakji
 */
public record AgentChatLayout(
        AgentChatGeometry window,
        AgentChatGeometry titleBar,
        AgentChatGeometry transcript,
        AgentChatGeometry inputBar,
        AgentChatGeometry profileButton,
        AgentChatGeometry newButton,
        AgentChatGeometry sendButton,
        AgentChatGeometry stopButton,
        AgentChatGeometry fullscreenButton,
        AgentChatGeometry closeButton,
        AgentChatGeometry resizeGrip) {
    public static final int TITLE_HEIGHT = 18;
    public static final int INPUT_HEIGHT = 24;
    public static final int BUTTON_SIZE = 18;
    public static final int MIN_WINDOW_WIDTH = 360;
    public static final int MIN_WINDOW_HEIGHT = 240;
    public static final int DEFAULT_WINDOW_WIDTH = 560;
    public static final int DEFAULT_WINDOW_HEIGHT = 380;
    public static final int DEFAULT_SCREEN_PERCENT = 70;
    public static final int FULLSCREEN_MARGIN = 8;
    public static final int RESIZE_GRIP = 12;
    private static final int ROW_PADDING = 3;
    private static final int PROFILE_WIDTH = 110;
    private static final int ACTION_WIDTH = 44;

    /**
     * Computes all chat rectangles for a window.
     *
     * @param window window bounds
     * @return layout rectangles
     */
    public static AgentChatLayout of(AgentChatGeometry window) {
        AgentChatGeometry title = new AgentChatGeometry(window.x(), window.y(), window.width(), TITLE_HEIGHT);
        AgentChatGeometry input =
                new AgentChatGeometry(window.x(), window.bottom() - INPUT_HEIGHT, window.width(), INPUT_HEIGHT);
        int transcriptHeight = Math.max(1, input.y() - title.bottom());
        AgentChatGeometry transcript =
                new AgentChatGeometry(window.x(), title.bottom(), window.width(), transcriptHeight);

        int buttonY = title.y();
        AgentChatGeometry close =
                new AgentChatGeometry(window.right() - BUTTON_SIZE, buttonY, BUTTON_SIZE, BUTTON_SIZE);
        AgentChatGeometry fullscreen =
                new AgentChatGeometry(close.x() - BUTTON_SIZE, buttonY, BUTTON_SIZE, BUTTON_SIZE);

        int inputY = input.y() + ROW_PADDING;
        int inputButtonHeight = Math.max(1, INPUT_HEIGHT - ROW_PADDING * 2);
        AgentChatGeometry profile = new AgentChatGeometry(window.x() + 4, inputY, PROFILE_WIDTH, inputButtonHeight);
        int actionY = input.y() + ROW_PADDING;
        int actionRightInset = RESIZE_GRIP + 4;
        AgentChatGeometry send = new AgentChatGeometry(
                window.right() - ACTION_WIDTH - actionRightInset, actionY, ACTION_WIDTH, inputButtonHeight);
        AgentChatGeometry stop =
                new AgentChatGeometry(send.x() - ACTION_WIDTH - 4, actionY, ACTION_WIDTH, inputButtonHeight);
        AgentChatGeometry newChat =
                new AgentChatGeometry(profile.right() + 4, actionY, ACTION_WIDTH, inputButtonHeight);
        AgentChatGeometry grip = new AgentChatGeometry(
                window.right() - RESIZE_GRIP, window.bottom() - RESIZE_GRIP, RESIZE_GRIP, RESIZE_GRIP);

        return new AgentChatLayout(
                window, title, transcript, input, profile, newChat, send, stop, fullscreen, close, grip);
    }
}
