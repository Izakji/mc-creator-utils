package com.izakji.agent.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class SseParserTest {

    @Test
    void reassemblesSplitDataLinesAndStopsAtDone() {
        SseParser parser = new SseParser();
        assertEquals(List.of(), parser.accept("data: {\"a\":"));
        assertEquals(List.of(new SseEvent("", "{\"a\":1}")), parser.accept("1}\n\ndata: [DONE]\n\n"));
        assertEquals(List.of(), parser.accept("data: {\"ignored\":true}\n\n"));
    }

    @Test
    void preservesEventNames() {
        SseParser parser = new SseParser();
        assertEquals(
                List.of(new SseEvent("content_block_delta", "{\"x\":1}")),
                parser.accept("event: content_block_delta\ndata: {\"x\":1}\n\n"));
    }
}
