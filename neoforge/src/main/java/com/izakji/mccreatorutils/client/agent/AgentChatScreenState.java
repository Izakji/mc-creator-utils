package com.izakji.mccreatorutils.client.agent;

import java.util.HashSet;
import java.util.Set;

/**
 * Scroll and collapse state for the floating chat screen.
 *
 * @author Izakji
 */
final class AgentChatScreenState {
    private int scrollOffset;
    private int contentHeight;
    private boolean followTail = true;
    private long collapsedRevision;
    private final Set<Integer> collapsedEntries = new HashSet<>();

    int scrollOffset() {
        return scrollOffset;
    }

    boolean followTail() {
        return followTail;
    }

    void scrollBy(int delta) {
        scrollOffset = Math.max(0, scrollOffset + delta);
        followTail = scrollOffset == 0;
    }

    void scrollToTail() {
        scrollOffset = 0;
        followTail = true;
    }

    void onTranscriptChanged() {
        // New content follows the tail only when the reader is already there.
    }

    void onContentMeasured(int height) {
        if (!followTail && contentHeight > 0) {
            scrollOffset += Math.max(0, height - contentHeight);
        }
        contentHeight = Math.max(0, height);
    }

    void clampToContent(int maxOffset) {
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, maxOffset)));
        followTail = scrollOffset == 0;
    }

    void reset() {
        scrollOffset = 0;
        contentHeight = 0;
        followTail = true;
        collapsedEntries.clear();
        collapsedRevision++;
    }

    long collapsedRevision() {
        return collapsedRevision;
    }

    boolean toggleCollapsed(int entryIndex) {
        collapsedRevision++;
        if (collapsedEntries.add(entryIndex)) {
            return true;
        }
        collapsedEntries.remove(entryIndex);
        return false;
    }

    boolean isCollapsed(int entryIndex) {
        return collapsedEntries.contains(entryIndex);
    }

    void clearCollapsed() {
        collapsedEntries.clear();
        collapsedRevision++;
    }
}
