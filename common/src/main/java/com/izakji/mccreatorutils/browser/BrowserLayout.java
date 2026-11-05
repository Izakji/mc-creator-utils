package com.izakji.mccreatorutils.browser;

/**
 * Layout rectangles for a browser window in GUI coordinates.
 *
 * @param window full window
 * @param titleBar draggable title bar
 * @param tabStrip tab row
 * @param navBar navigation row
 * @param addressBar address row
 * @param content browser viewport
 * @param resizeGrip bottom-right resize handle
 * @author Izakji
 */
public record BrowserLayout(
        BrowserGeometry window,
        BrowserGeometry titleBar,
        BrowserGeometry tabStrip,
        BrowserGeometry navBar,
        BrowserGeometry addressBar,
        BrowserGeometry content,
        BrowserGeometry resizeGrip) {
    public static final int TITLE_HEIGHT = 16;
    public static final int TAB_HEIGHT = 16;
    public static final int NAV_HEIGHT = 20;
    public static final int NAV_BUTTON_WIDTH = 20;
    public static final int NAV_BUTTON_GAP = 4;
    public static final int NAV_PADDING = 2;
    public static final int NAV_BUTTON_COUNT = 3;
    public static final int ADDRESS_HEIGHT = 16;
    public static final int MIN_WINDOW_WIDTH = 320;
    public static final int MIN_WINDOW_HEIGHT = 220;
    public static final int DEFAULT_WINDOW_WIDTH = 480;
    public static final int DEFAULT_WINDOW_HEIGHT = 320;
    public static final int DEFAULT_SCREEN_PERCENT = 70;
    public static final int CONTENT_MARGIN = 2;
    public static final int RESIZE_GRIP = 12;

    /**
     * Computes all browser rectangles for a window.
     *
     * @param window window bounds
     * @return layout rectangles
     */
    public static BrowserLayout of(BrowserGeometry window) {
        int x = window.x();
        int y = window.y();
        int width = window.width();
        BrowserGeometry title = new BrowserGeometry(x, y, width, TITLE_HEIGHT);
        BrowserGeometry tabs = new BrowserGeometry(x, y + TITLE_HEIGHT, width, TAB_HEIGHT);
        BrowserGeometry nav = new BrowserGeometry(x, y + TITLE_HEIGHT + TAB_HEIGHT, width, NAV_HEIGHT);
        BrowserGeometry address =
                new BrowserGeometry(x, y + TITLE_HEIGHT + TAB_HEIGHT + NAV_HEIGHT, width, ADDRESS_HEIGHT);
        int contentY = y + TITLE_HEIGHT + TAB_HEIGHT + NAV_HEIGHT + ADDRESS_HEIGHT + CONTENT_MARGIN;
        int contentHeight =
                window.height() - (TITLE_HEIGHT + TAB_HEIGHT + NAV_HEIGHT + ADDRESS_HEIGHT + CONTENT_MARGIN * 2);
        BrowserGeometry content = new BrowserGeometry(
                x + CONTENT_MARGIN, contentY, Math.max(1, width - CONTENT_MARGIN * 2), Math.max(1, contentHeight));
        BrowserGeometry grip = new BrowserGeometry(
                window.right() - RESIZE_GRIP, window.bottom() - RESIZE_GRIP, RESIZE_GRIP, RESIZE_GRIP);
        return new BrowserLayout(window, title, tabs, nav, address, content, grip);
    }

    /**
     * Returns the left edge of one navigation button, including the leading padding and gaps.
     *
     * @param navBar navigation bar rectangle
     * @param index zero-based button index
     * @return left GUI coordinate
     */
    public static int navButtonLeft(BrowserGeometry navBar, int index) {
        return navBar.x() + NAV_PADDING + index * (NAV_BUTTON_WIDTH + NAV_BUTTON_GAP);
    }

    /**
     * Returns which navigation button contains a horizontal coordinate.
     *
     * @param navBar navigation bar rectangle
     * @param mouseX GUI x coordinate
     * @return zero-based button index, or -1 when no button is under the cursor
     */
    public static int navButtonAt(BrowserGeometry navBar, double mouseX) {
        for (int index = 0; index < NAV_BUTTON_COUNT; index++) {
            int left = navButtonLeft(navBar, index);
            if (mouseX >= left && mouseX < left + NAV_BUTTON_WIDTH) {
                return index;
            }
        }
        return -1;
    }
}
