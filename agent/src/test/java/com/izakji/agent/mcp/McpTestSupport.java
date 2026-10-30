package com.izakji.agent.mcp;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;

final class McpTestSupport {
    private McpTestSupport() {}

    static McpServerConfig config(String id) {
        return new McpServerConfig(
                id, id, McpTransportType.STDIO, "fake", List.of(), null, Map.of(), null, Map.of(), true, false);
    }

    static JsonObject schema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        return schema;
    }

    static McpToolDescriptor descriptor(String name, boolean readOnly, boolean destructive) {
        return new McpToolDescriptor(name, name, schema(), readOnly, destructive);
    }
}
