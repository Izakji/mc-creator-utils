package com.izakji.agent.mcp;

import com.google.gson.JsonObject;

/**
 * MCP prompt descriptor.
 *
 * @param name prompt name
 * @param description description
 * @param argumentsSchema JSON Schema for prompt arguments
 * @author Izakji
 */
public record McpPromptDescriptor(String name, String description, JsonObject argumentsSchema) {
    public McpPromptDescriptor {
        argumentsSchema = argumentsSchema == null ? new JsonObject() : argumentsSchema.deepCopy();
    }

    @Override
    public JsonObject argumentsSchema() {
        return argumentsSchema.deepCopy();
    }
}
