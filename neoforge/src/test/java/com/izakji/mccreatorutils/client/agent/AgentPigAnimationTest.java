package com.izakji.mccreatorutils.client.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AgentPigAnimationTest {

    @Test
    void cyclesThroughFrames() {
        assertEquals(0, AgentPigAnimation.frame(0));
        assertEquals(1, AgentPigAnimation.frame(160));
        assertEquals(2, AgentPigAnimation.frame(320));
        assertEquals(3, AgentPigAnimation.frame(480));
        assertEquals(0, AgentPigAnimation.frame(640));
    }
}
