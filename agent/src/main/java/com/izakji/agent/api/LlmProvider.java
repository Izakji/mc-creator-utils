package com.izakji.agent.api;

import com.izakji.agent.config.ProviderType;
import java.util.concurrent.CompletableFuture;

/**
 * Provider protocol adapter.
 *
 * @author Izakji
 */
public interface LlmProvider {
    ProviderType type();

    boolean supportsTools();

    CompletableFuture<Void> stream(
            ChatRequest request, StreamListener listener, CancellationToken cancellationToken);
}
