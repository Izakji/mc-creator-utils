package com.izakji.agent.mcp;

import com.izakji.agent.api.ContentPart;
import java.util.List;

/**
 * MCP tool call result.
 *
 * @param parts result content
 * @param error whether the server marked the call as an error
 * @author Izakji
 */
public record McpCallResult(List<ContentPart> parts, boolean error) {
    public McpCallResult {
        parts = List.copyOf(parts);
    }
}
