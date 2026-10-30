package com.izakji.agent.runtime;

import com.izakji.agent.api.ToolContext;
import java.util.concurrent.CompletableFuture;

/**
 * Supplies a bounded live snapshot for the system prompt.
 *
 * @author Izakji
 */
public interface ContextProvider {
    String id();

    CompletableFuture<String> snapshot(ToolContext context);
}
