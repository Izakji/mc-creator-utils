package com.izakji.mccreatorutils.locate.engine;

import java.util.concurrent.CancellationException;

/**
 * Cooperative cancellation signal for a locate task.
 *
 * @author Izakji
 */
@FunctionalInterface
public interface CancellationToken {

    boolean isCancelled();

    default void throwIfCancelled() {
        if (isCancelled()) {
            throw new CancellationException("locate request cancelled");
        }
    }
}
