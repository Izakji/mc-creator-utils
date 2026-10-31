package com.izakji.mccreatorutils.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AgentChatGeometryTest {

    @Test
    void centersAndClamps() {
        AgentChatGeometry centered = AgentChatGeometry.centered(854, 480, 560, 380);
        assertEquals(147, centered.x());
        assertEquals(50, centered.y());

        AgentChatGeometry clamped = new AgentChatGeometry(-50, -20, 900, 700).clampTo(400, 300, 360, 240);
        assertEquals(0, clamped.x());
        assertEquals(0, clamped.y());
        assertEquals(400, clamped.width());
        assertEquals(300, clamped.height());
    }

    @Test
    void fillsTheScreenWithAMargin() {
        assertEquals(new AgentChatGeometry(8, 8, 838, 464), AgentChatGeometry.fullscreen(854, 480, 8));
    }

    @Test
    void exposesEdges() {
        AgentChatGeometry geometry = new AgentChatGeometry(10, 20, 100, 50);
        assertEquals(110, geometry.right());
        assertEquals(70, geometry.bottom());
    }
}
