package com.izakji.mccreatorutils.browser;

import java.util.Objects;

/**
 * Shared tab-strip geometry for rendering and hit testing.
 *
 * <p>Every tab is laid out at the same width. The width shrinks below the preferred minimum when the
 * strip is narrow, so all {@link BrowserTabRules#MAX_TABS} tabs remain visible and clickable at
 * {@link BrowserLayout#MIN_WINDOW_WIDTH}.
 *
 * @author Izakji
 */
public final class BrowserTabLayout {
    /** Left inset before the first tab. */
    public static final int TAB_START = 2;

    /** Horizontal space between adjacent tabs. */
    public static final int TAB_GAP = 2;

    /** Vertical inset above and below each tab. */
    public static final int TAB_VERTICAL_MARGIN = 2;

    /** Width reserved for the new-tab button. */
    public static final int PLUS_WIDTH = 16;

    /** Space between the last tab and the plus button. */
    public static final int PLUS_MARGIN = 4;

    /** Preferred width of the right-edge close zone. */
    public static final int TAB_CLOSE_WIDTH = 12;

    private BrowserTabLayout() {}

    /**
     * Returns the width shared by every tab in the strip.
     *
     * @param strip tab-strip rectangle
     * @param tabCount number of visible tabs
     * @return tab width, or zero when there are no tabs
     */
    public static int tabWidth(BrowserGeometry strip, int tabCount) {
        Objects.requireNonNull(strip, "strip");
        if (tabCount <= 0) {
            return 0;
        }
        int available = strip.width() - TAB_START - PLUS_WIDTH - PLUS_MARGIN;
        int width = (available - (tabCount - 1) * TAB_GAP) / tabCount;
        return Math.max(1, width);
    }

    /**
     * Returns one tab rectangle inside the strip.
     *
     * @param strip tab-strip rectangle
     * @param index zero-based tab index
     * @param tabCount number of visible tabs
     * @return tab rectangle
     * @throws IndexOutOfBoundsException when {@code index} is outside {@code [0, tabCount)}
     */
    public static BrowserGeometry tabBounds(BrowserGeometry strip, int index, int tabCount) {
        Objects.requireNonNull(strip, "strip");
        if (index < 0 || index >= tabCount) {
            throw new IndexOutOfBoundsException("tab index " + index + " outside [0, " + tabCount + ")");
        }
        int width = tabWidth(strip, tabCount);
        int x = strip.x() + TAB_START + index * (width + TAB_GAP);
        return new BrowserGeometry(
                x, strip.y() + TAB_VERTICAL_MARGIN, width, Math.max(1, strip.height() - TAB_VERTICAL_MARGIN * 2));
    }

    /**
     * Returns the rectangle occupied by the new-tab button.
     *
     * @param strip tab-strip rectangle
     * @return plus-button rectangle
     */
    public static BrowserGeometry plusBounds(BrowserGeometry strip) {
        Objects.requireNonNull(strip, "strip");
        int width = Math.min(PLUS_WIDTH, Math.max(1, strip.width()));
        return new BrowserGeometry(
                strip.right() - width,
                strip.y() + TAB_VERTICAL_MARGIN,
                width,
                Math.max(1, strip.height() - TAB_VERTICAL_MARGIN * 2));
    }

    /**
     * Returns whether a horizontal position hits the tab's close zone.
     *
     * @param tab tab rectangle
     * @param mouseX horizontal mouse position
     * @return true when the position is inside the right-edge close zone
     */
    public static boolean isCloseHit(BrowserGeometry tab, double mouseX) {
        Objects.requireNonNull(tab, "tab");
        int closeWidth = Math.min(TAB_CLOSE_WIDTH, Math.max(1, tab.width() / 2));
        return mouseX >= tab.right() - closeWidth && mouseX < tab.right();
    }
}
