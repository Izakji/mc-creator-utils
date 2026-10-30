package com.izakji.agent.api;

import java.util.Objects;

/**
 * System instruction message.
 *
 * @param text instruction text
 * @author Izakji
 */
public record SystemMessage(String text) implements ChatMessage {
    public SystemMessage {
        text = Objects.requireNonNull(text, "text");
    }

    @Override
    public ChatRole role() {
        return ChatRole.SYSTEM;
    }
}
