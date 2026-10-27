package com.izakji.mccreatorutils.locate.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ClimateCoordinatesTest {

    @Test
    void alignsToTheQuartBlockVanillaSamples() {
        assertEquals(0, ClimateCoordinates.alignToQuart(0));
        assertEquals(0, ClimateCoordinates.alignToQuart(1));
        assertEquals(0, ClimateCoordinates.alignToQuart(3));
        assertEquals(4, ClimateCoordinates.alignToQuart(4));
        assertEquals(-4, ClimateCoordinates.alignToQuart(-1));
        assertEquals(164, ClimateCoordinates.alignToQuart(167));
    }
}
