package com.izakji.agent.api;

import java.util.List;
import java.util.Objects;

/**
 * User message.
 *
 * @param parts content parts
 * @author Izakji
 */
public record UserMessage(List<ContentPart> parts) implements ChatMessage {
    public UserMessage {
        parts = List.copyOf(Objects.requireNonNull(parts, "parts"));
    }

    @Override
    public ChatRole role() {
        return ChatRole.USER;
    }
}
