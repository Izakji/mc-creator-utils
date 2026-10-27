package com.izakji.mccreatorutils.locate.engine;

import static com.izakji.mccreatorutils.locate.engine.TestClimate.point;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BiomeEnvelopeTest {

    @Test
    void boundsAndFixedAxesAreValid() {
        var envelope = BiomeEnvelope.of(List.of(
                point(0, 100, 0, 100, 0, 100, 0, 100, 0, 100),
                point(20, 30, 40, 50, 60, 70, 80, 90, 100, 200)));

        assertTrue(envelope.contains(20, 40, 60, 80, 100));
        assertFalse(envelope.contains(201, 100, 100, 100, 100));
        assertFalse(envelope.isImpossible());
        assertEquals(ClimateAxis.TEMPERATURE, envelope.selectivityOrder().get(0));
    }

    @Test
    void emptyEnvelopeIsImpossibleAndUnionExpands() {
        assertTrue(BiomeEnvelope.of(List.of()).isImpossible());

        var union = BiomeEnvelope.union(
                BiomeEnvelope.of(List.of(point(0, 10, 0, 10, 0, 10, 0, 10, 0, 10))),
                BiomeEnvelope.of(List.of(point(20, 30, 20, 30, 20, 30, 20, 30, 20, 30))));

        assertTrue(union.contains(5, 5, 5, 5, 5));
        assertTrue(union.contains(25, 25, 25, 25, 25));
        assertTrue(union.contains(15, 15, 15, 15, 15));
        assertFalse(union.contains(31, 5, 5, 5, 5));
    }
}
