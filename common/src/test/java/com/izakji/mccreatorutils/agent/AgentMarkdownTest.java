package com.izakji.mccreatorutils.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class AgentMarkdownTest {

    @Test
    void parsesBoldItalicAndCodeSpans() {
        assertEquals(
                List.of(
                        new AgentMarkdown.Span("bold", true, false, false),
                        new AgentMarkdown.Span(" and ", false, false, false),
                        new AgentMarkdown.Span("italic", false, true, false),
                        new AgentMarkdown.Span(" and ", false, false, false),
                        new AgentMarkdown.Span("code", false, false, true)),
                AgentMarkdown.parseLine("**bold** and *italic* and `code`"));
    }

    @Test
    void leavesUnmatchedMarkersLiteral() {
        assertEquals(List.of(new AgentMarkdown.Span("a ** b", false, false, false)), AgentMarkdown.parseLine("a ** b"));
        assertEquals(List.of(new AgentMarkdown.Span("a ` b", false, false, false)), AgentMarkdown.parseLine("a ` b"));
        assertEquals(
                List.of(new AgentMarkdown.Span("**bold*", false, false, false)), AgentMarkdown.parseLine("**bold*"));
    }

    @Test
    void handlesEmptyText() {
        assertEquals(List.of(), AgentMarkdown.parseLine(""));
    }
}
