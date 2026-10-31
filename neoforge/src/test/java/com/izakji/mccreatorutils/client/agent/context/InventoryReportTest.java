package com.izakji.mccreatorutils.client.agent.context;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class InventoryReportTest {

    @Test
    void listsSlotsIdsCountsAndNames() {
        String report = InventoryReport.format(List.of(
                new InventoryEntry(0, "minecraft:stone", 64, "Stone", 0),
                new InventoryEntry(1, "minecraft:diamond_sword", 1, "Diamond Sword", 10)));
        assertTrue(report.contains("minecraft:stone x64"));
        assertTrue(report.contains("minecraft:diamond_sword"));
        assertTrue(report.contains("Diamond Sword"));
        assertTrue(report.contains("slot 0"));
    }

    @Test
    void reportsEmptyInventory() {
        assertTrue(InventoryReport.format(List.of()).contains("Inventory is empty"));
    }
}
