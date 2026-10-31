package com.izakji.mccreatorutils.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AgentChatLayoutTest {

    @Test
    void laysOutTitleTranscriptInputAndGrip() {
        AgentChatLayout layout = AgentChatLayout.of(new AgentChatGeometry(10, 20, 560, 380));
        assertEquals(18, layout.titleBar().height());
        assertEquals(layout.titleBar().bottom(), layout.transcript().y());
        assertEquals(layout.inputBar().y(), layout.transcript().bottom());
        assertEquals(layout.window().bottom(), layout.inputBar().bottom());
        assertEquals(layout.window().right(), layout.resizeGrip().right());
        assertEquals(layout.window().bottom(), layout.resizeGrip().bottom());
    }

    @Test
    void placesTitleAndInputButtonsInsideTheirRows() {
        AgentChatLayout layout = AgentChatLayout.of(new AgentChatGeometry(10, 20, 560, 380));
        assertTrue(layout.closeButton().right() <= layout.titleBar().right());
        assertTrue(layout.fullscreenButton().right() <= layout.closeButton().x());
        assertTrue(layout.profileButton().x() >= layout.inputBar().x());
        assertTrue(layout.sendButton().right() <= layout.inputBar().right());
        assertTrue(layout.stopButton().right() <= layout.inputBar().right());
        assertTrue(layout.newButton().right() <= layout.inputBar().right());
    }
}
