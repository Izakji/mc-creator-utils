package com.izakji.mccreatorutils.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AgentChatWindowStateTest {

    @Test
    void fullscreenRestoresPreviousBounds() {
        AgentChatWindowState state = new AgentChatWindowState();
        state.show(854, 480);
        AgentChatGeometry before = state.bounds(854, 480);
        state.toggleFullscreen(854, 480);
        assertEquals(new AgentChatGeometry(8, 8, 838, 464), state.bounds(854, 480));
        state.toggleFullscreen(854, 480);
        assertEquals(before, state.bounds(854, 480));
    }

    @Test
    void hidingKeepsBoundsButReportsNotVisible() {
        AgentChatWindowState state = new AgentChatWindowState();
        state.show(854, 480);
        AgentChatGeometry shown = state.bounds(854, 480);
        assertTrue(state.visible());
        state.hide();
        assertFalse(state.visible());
        state.show(854, 480);
        assertEquals(shown, state.bounds(854, 480));
    }

    @Test
    void setBoundsClampsToTheScreen() {
        AgentChatWindowState state = new AgentChatWindowState();
        state.show(854, 480);
        state.setBounds(new AgentChatGeometry(-40, 900, 50, 50), 854, 480);
        AgentChatGeometry bounds = state.bounds(854, 480);
        assertEquals(0, bounds.x());
        assertEquals(480 - bounds.height(), bounds.y());
        assertEquals(360, bounds.width());
        assertEquals(240, bounds.height());
    }
}
