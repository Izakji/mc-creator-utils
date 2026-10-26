package com.izakji.mccreatorutils.tool;

import java.util.Objects;

/**
 * Describes one tool shown in the Creator Utils navigation pane.
 *
 * @param id stable tool identifier
 * @param translationKey language key for the tool name
 * @param requiredModId optional mod id that must be present for the tool to work
 * @author Izakji
 */
public record ToolDescriptor(ToolId id, String translationKey, String requiredModId) {
    public ToolDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(translationKey, "translationKey");
        requiredModId = requiredModId == null ? "" : requiredModId;
    }

    public boolean hasRequiredMod() {
        return !requiredModId.isEmpty();
    }
}
