package com.izakji.agent.api;

import java.util.concurrent.CancellationException;

/**
 * Cooperative cancellation signal.
 *
 * @author Izakji
 */
@FunctionalInterface
public interface CancellationToken {
    boolean isCancelled();

    default void throwIfCancelled() {
        if (isCancelled()) {
            throw new CancellationException("cancelled");
        }
    }
}
