package com.izakji.mccreatorutils.browser;

import java.net.URI;
import java.util.List;
import java.util.Objects;
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
            if (samePage(urls.get(index), normalizedUrl)) {
                return OptionalInt.of(index);
            }
        }
        return OptionalInt.empty();
    }

    private static boolean samePage(String left, String right) {
        if (left == null || right == null) {
            return left == null && right == null;
        }
        String normalizedLeft = BrowserUrlInput.normalize(left).orElse(left);
        String normalizedRight = BrowserUrlInput.normalize(right).orElse(right);
        if (normalizedLeft.equals(normalizedRight)) {
            return true;
        }
        try {
            URI leftUri = URI.create(normalizedLeft);
            URI rightUri = URI.create(normalizedRight);
            return equalsIgnoreCase(leftUri.getScheme(), rightUri.getScheme())
                    && equalsIgnoreCase(leftUri.getHost(), rightUri.getHost())
                    && leftUri.getPort() == rightUri.getPort()
                    && Objects.equals(rootPath(leftUri.getRawPath()), rootPath(rightUri.getRawPath()))
                    && Objects.equals(leftUri.getRawQuery(), rightUri.getRawQuery())
                    && Objects.equals(leftUri.getRawFragment(), rightUri.getRawFragment());
        } catch (IllegalArgumentException malformed) {
            return false;
        }
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        return left == null ? right == null : right != null && left.equalsIgnoreCase(right);
    }

    private static String rootPath(String path) {
        return path == null || path.isEmpty() ? "/" : path;
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
