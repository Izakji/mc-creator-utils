package com.izakji.mccreatorutils.client.agent.context;

import java.util.List;

/**
 * Formats non-empty inventory slots.
 *
 * @author Izakji
 */
public final class InventoryReport {
    private InventoryReport() {}

    public static String format(List<InventoryEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return "Inventory is empty.";
        }
        StringBuilder report = new StringBuilder();
        report.append("Inventory (").append(entries.size()).append(" occupied slots):\n");
        for (InventoryEntry entry : entries) {
            report.append("- slot ")
                    .append(entry.slot())
                    .append(": ")
                    .append(entry.itemId())
                    .append(" x")
                    .append(entry.count())
                    .append(" (")
                    .append(entry.name())
                    .append(')');
            if (entry.damage() > 0) {
                report.append(" damage ").append(entry.damage());
            }
            report.append('\n');
        }
        return report.toString();
    }
}
