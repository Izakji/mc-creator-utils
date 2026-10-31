package com.izakji.mccreatorutils.client.agent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AgentChatScreenStateTest {

    @Test
    void scrollingUpPausesAutoScrollAndNewDeltaKeepsPause() {
        AgentChatScreenState state = new AgentChatScreenState();
        assertTrue(state.followTail());
        state.scrollBy(-30);
        assertFalse(state.followTail());
        state.onTranscriptChanged();
        assertFalse(state.followTail());
        state.scrollToTail();
        assertTrue(state.followTail());
    }
}
