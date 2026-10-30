package com.izakji.agent.mcp;

import com.izakji.agent.api.ChatMessage;
import java.util.List;

/**
 * MCP prompt result.
 *
 * @param description prompt description
 * @param messages prompt messages
 * @author Izakji
 */
public record McpPromptResult(String description, List<ChatMessage> messages) {
    public McpPromptResult {
        messages = List.copyOf(messages);
    }
}
