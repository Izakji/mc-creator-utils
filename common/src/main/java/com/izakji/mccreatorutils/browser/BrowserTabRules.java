package com.izakji.mccreatorutils.browser;

import java.util.List;
import java.util.OptionalInt;

/**
 * Pure tab-list rules for the browser workspace.
 *
 * @author Izakji
 */
public final class BrowserTabRules {
    public static final int MAX_TABS = 8;

    private BrowserTabRules() {}

    /**
     * Returns whether another tab can be opened.
     *
     * @param currentCount current number of tabs
     * @return true when below the maximum
     */
    public static boolean canOpenNewTab(int currentCount) {
        return currentCount < MAX_TABS;
    }

    /**
     * Finds an already-open normalized URL.
     *
     * @param urls tab URLs
     * @param normalizedUrl URL to find
     * @return matching tab index, or empty
     */
    public static OptionalInt findOpenTab(List<String> urls, String normalizedUrl) {
        for (int index = 0; index < urls.size(); index++) {
            if (urls.get(index).equals(normalizedUrl)) {
                return OptionalInt.of(index);
            }
        }
        return OptionalInt.empty();
    }

    /**
     * Chooses the active index after one tab is removed.
     *
     * @param currentIndex active index before removal
     * @param removedIndex removed index
     * @param oldSize tab count before removal
     * @return next active index, or zero when no tabs remain
     */
    public static int nextActiveIndex(int currentIndex, int removedIndex, int oldSize) {
        if (oldSize <= 1) {
            return 0;
        }
        if (removedIndex < currentIndex) {
            return currentIndex - 1;
        }
        if (removedIndex == currentIndex) {
            return Math.min(currentIndex, oldSize - 2);
        }
        return Math.min(currentIndex, oldSize - 2);
    }
}
