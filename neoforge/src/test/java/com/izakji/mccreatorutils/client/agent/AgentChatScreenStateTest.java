package com.izakji.mccreatorutils.client.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AgentChatScreenStateTest {

    @Test
    void scrollingUpPausesAutoScrollAndScrollingBackFollowsTheTail() {
        AgentChatScreenState state = new AgentChatScreenState();
        assertTrue(state.followTail());
        state.scrollBy(30);
        assertFalse(state.followTail());
        state.onTranscriptChanged();
        assertFalse(state.followTail());
        state.scrollBy(-30);
        assertTrue(state.followTail());
        state.scrollToTail();
        assertTrue(state.followTail());
    }

    @Test
    void scrolledViewStaysAnchoredWhenContentGrows() {
        AgentChatScreenState state = new AgentChatScreenState();
        state.onContentMeasured(100);
        state.scrollBy(30);
        state.onContentMeasured(140);
        assertEquals(70, state.scrollOffset());
        state.clampToContent(60);
        assertEquals(60, state.scrollOffset());
    }
}
