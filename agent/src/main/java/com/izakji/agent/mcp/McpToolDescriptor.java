package com.izakji.agent.mcp;

import com.google.gson.JsonObject;

/**
 * Tool advertised by an MCP server.
 *
 * @param name tool name
 * @param description description
 * @param inputSchema JSON Schema
 * @param readOnly read-only annotation
 * @param destructive destructive annotation
 * @author Izakji
 */
public record McpToolDescriptor(
        String name, String description, JsonObject inputSchema, boolean readOnly, boolean destructive) {
    public McpToolDescriptor {
        inputSchema = inputSchema == null ? new JsonObject() : inputSchema.deepCopy();
    }

    @Override
    public JsonObject inputSchema() {
        return inputSchema.deepCopy();
    }
}
