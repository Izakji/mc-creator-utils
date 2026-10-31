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
    private boolean followTail = true;
    private final Set<Integer> collapsedEntries = new HashSet<>();

    int scrollOffset() {
        return scrollOffset;
    }

    boolean followTail() {
        return followTail;
    }

    void scrollBy(int delta) {
        scrollOffset = Math.max(0, scrollOffset + delta);
        if (delta < 0) {
            followTail = false;
        }
    }

    void scrollToTail() {
        scrollOffset = 0;
        followTail = true;
    }

    void onTranscriptChanged() {
        // New content follows the tail only when the reader is already there.
    }

    boolean toggleCollapsed(int entryIndex) {
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
    }
}
