package com.izakji.mccreatorutils.client.agent.context;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RecipeReportTest {

    @Test
    void findsByOutputAndShowsInputs() {
        RecipeEntry entry = new RecipeEntry(
                "minecraft:oak_planks", "minecraft:crafting", "minecraft:oak_planks", 4, List.of("minecraft:oak_log"));
        String report = RecipeReport.format(List.of(entry), "oak_planks", 10);
        assertTrue(report.contains("minecraft:oak_planks"));
        assertTrue(report.contains("minecraft:oak_log"));
        assertTrue(report.contains("x4"));
    }

    @Test
    void reportsNoMatches() {
        assertTrue(RecipeReport.format(List.of(), "nothing", 10).contains("No matching recipes"));
    }
}
