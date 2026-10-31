package com.izakji.mccreatorutils.client.agent.context;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NumberStatsTest {

    @Test
    void summarizesBoundedValues() {
        NumberStats stats = NumberStats.of(new long[] {10, 20, 30, 40, 50});
        assertEquals(5, stats.count());
        assertEquals(30.0, stats.average());
        assertEquals(10, stats.min());
        assertEquals(30, stats.median());
        assertEquals(50, stats.p95());
        assertEquals(50, stats.max());
    }

    @Test
    void handlesEmptyValues() {
        assertEquals(0, NumberStats.of(new long[0]).count());
    }
}
