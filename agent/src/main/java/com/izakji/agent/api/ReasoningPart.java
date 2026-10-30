package com.izakji.agent.api;

import java.util.Objects;

/**
 * Reasoning/thinking content returned by a provider.
 *
 * @param text reasoning text
 * @param signature provider-specific continuation signature, or an empty string
 * @author Izakji
 */
public record ReasoningPart(String text, String signature) implements ContentPart {
    public ReasoningPart {
        text = Objects.requireNonNull(text, "text");
        signature = signature == null ? "" : signature;
    }

    @Override
    public String type() {
        return "reasoning";
    }
}
