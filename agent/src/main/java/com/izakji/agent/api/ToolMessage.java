package com.izakji.agent.api;

import java.util.List;
import java.util.Objects;

/**
 * Tool result message.
 *
 * @param toolCallId id of the call this result answers
 * @param name tool name
 * @param parts result content
 * @param error whether the result describes a failure
 * @author Izakji
 */
public record ToolMessage(String toolCallId, String name, List<ContentPart> parts, boolean error)
        implements ChatMessage {
    public ToolMessage {
        toolCallId = Objects.requireNonNull(toolCallId, "toolCallId");
        name = Objects.requireNonNull(name, "name");
        parts = List.copyOf(Objects.requireNonNull(parts, "parts"));
    }

    @Override
    public ChatRole role() {
        return ChatRole.TOOL;
    }
}
