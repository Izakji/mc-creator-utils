package com.izakji.agent.api;

import java.util.Objects;

/**
 * Visible text content.
 *
 * @param text text
 * @author Izakji
 */
public record TextPart(String text) implements ContentPart {
    public TextPart {
        text = Objects.requireNonNull(text, "text");
    }

    @Override
    public String type() {
        return "text";
    }
}
