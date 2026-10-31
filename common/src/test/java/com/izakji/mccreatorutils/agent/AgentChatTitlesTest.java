package com.izakji.mccreatorutils.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AgentChatTitlesTest {

    @Test
    void usesTheFirstLineAndBoundsIt() {
        assertEquals("Find diamonds", AgentChatTitles.fromFirstMessage("Find diamonds\nplease"));
        assertEquals("New Chat", AgentChatTitles.fromFirstMessage("   "));
        assertEquals(80, AgentChatTitles.fromFirstMessage("x".repeat(200)).length());
    }

    @Test
    void collapsesWhitespace() {
        assertEquals("Find diamonds", AgentChatTitles.fromFirstMessage("  Find   diamonds  "));
    }
}
