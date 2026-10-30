package com.izakji.agent.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cancellation and completion handle for one turn.
 *
 * @author Izakji
 */
public final class TurnHandle {
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final CompletableFuture<TurnResult> completion = new CompletableFuture<>();
    private volatile Runnable cancelAction = () -> {};

    public void cancel() {
        cancelled.set(true);
        cancelAction.run();
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public CompletableFuture<TurnResult> completion() {
        return completion;
    }

    void setCancelAction(Runnable action) {
        cancelAction = action;
    }

    void complete(TurnResult result) {
        completion.complete(result);
    }
}
