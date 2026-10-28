package com.izakji.mccreatorutils.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class SearchFilterTest {

    @Test
    void blankQueryReturnsAllValues() {
        assertEquals(List.of("alpha", "beta"), SearchFilter.filter(List.of("alpha", "beta"), " ", value -> value));
        assertEquals(List.of("alpha", "beta"), SearchFilter.filter(List.of("alpha", "beta"), null, value -> value));
    }

    @Test
    void filtersCaseInsensitivelyBySubstring() {
        assertEquals(
                List.of("minecraft:the_nether", "modded:nether_isles"),
                SearchFilter.filter(
                        List.of("minecraft:overworld", "minecraft:the_nether", "modded:nether_isles"),
                        "NETHER",
                        value -> value));
    }

    @Test
    void preservesCandidateOrder() {
        assertEquals(List.of("beta", "alpha"), SearchFilter.filter(List.of("beta", "alpha"), "a", value -> value));
    }
}
