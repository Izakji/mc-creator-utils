package com.izakji.mccreatorutils.client.agent.context;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ModListReportTest {

    @Test
    void listsMatchingModsWithVersionsAndAuthors() {
        List<ModEntry> mods = List.of(
                new ModEntry("terralith", "Terralith", "2.6.2", "Starmute"),
                new ModEntry("create", "Create", "0.5.1", "simibubi"));
        String report = ModListReport.format(mods, "terra", 50);
        assertTrue(report.contains("terralith"));
        assertTrue(report.contains("Terralith"));
        assertTrue(report.contains("2.6.2"));
        assertTrue(report.contains("Starmute"));
        assertTrue(!report.contains("create"));
    }

    @Test
    void capsResultsAndExplainsTruncation() {
        List<ModEntry> mods = List.of(
                new ModEntry("a", "A", "1", ""), new ModEntry("b", "B", "1", ""), new ModEntry("c", "C", "1", ""));
        String report = ModListReport.format(mods, "", 2);
        assertTrue(report.contains("2 of 3"));
    }
}
