package com.izakji.agent.api;

import java.util.List;
import java.util.Objects;

/**
 * Assistant message, optionally carrying tool calls.
 *
 * @param parts content parts
 * @param toolCalls tool calls requested by the model
 * @author Izakji
 */
public record AssistantMessage(List<ContentPart> parts, List<ToolCall> toolCalls) implements ChatMessage {
    public AssistantMessage {
        parts = List.copyOf(Objects.requireNonNull(parts, "parts"));
        toolCalls = List.copyOf(Objects.requireNonNull(toolCalls, "toolCalls"));
    }

    @Override
    public ChatRole role() {
        return ChatRole.ASSISTANT;
    }
}
