package com.izakji.agent.api;

import com.google.gson.JsonObject;
import java.util.Objects;

/**
 * Tool description exposed to a provider.
 *
 * @param name tool name
 * @param description tool description
 * @param inputSchema JSON Schema for arguments
 * @author Izakji
 */
public record ToolSchema(String name, String description, JsonObject inputSchema) {
    public ToolSchema {
        name = Objects.requireNonNull(name, "name");
        description = description == null ? "" : description;
        inputSchema = Objects.requireNonNull(inputSchema, "inputSchema").deepCopy();
    }

    @Override
    public JsonObject inputSchema() {
        return inputSchema.deepCopy();
    }
}
