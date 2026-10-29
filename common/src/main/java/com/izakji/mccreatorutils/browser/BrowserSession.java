package com.izakji.mccreatorutils.browser;

import java.util.List;

/**
 * In-memory browser tab snapshot for the current client session.
 *
 * @author Izakji
 */
public final class BrowserSession {
    private List<String> urls = List.of();
    private int activeIndex = -1;

    /**
     * Saves tab URLs and the active tab.
     *
     * @param urls tab URLs
     * @param activeIndex active tab index
     */
    public void save(List<String> urls, int activeIndex) {
        this.urls = List.copyOf(urls);
        this.activeIndex = this.urls.isEmpty() ? -1 : Math.max(0, Math.min(activeIndex, this.urls.size() - 1));
    }

    /**
     * Returns whether a restorable page snapshot exists.
     *
     * @return true when at least one tab was saved
     */
    public boolean hasPages() {
        return !urls.isEmpty();
    }

    /**
     * Returns the saved tab URLs.
     *
     * @return immutable tab URLs
     */
    public List<String> urls() {
        return urls;
    }

    /**
     * Returns the saved active tab index.
     *
     * @return active index, or -1 when empty
     */
    public int activeIndex() {
        return activeIndex;
    }

    /**
     * Clears the saved snapshot.
     */
    public void clear() {
        urls = List.of();
        activeIndex = -1;
    }
}
