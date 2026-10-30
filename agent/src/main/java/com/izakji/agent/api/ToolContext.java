package com.izakji.agent.api;

import java.util.Objects;

/**
 * Execution context for one tool call.
 *
 * @param conversationId conversation id
 * @param cancellationToken cancellation signal
 * @author Izakji
 */
public record ToolContext(String conversationId, CancellationToken cancellationToken) {
    public ToolContext {
        conversationId = Objects.requireNonNull(conversationId, "conversationId");
        cancellationToken = Objects.requireNonNull(cancellationToken, "cancellationToken");
    }
}
