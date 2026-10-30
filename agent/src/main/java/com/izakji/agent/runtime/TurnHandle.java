package com.izakji.agent.runtime;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cancellation and completion handle for one turn.
 *
 * @author Izakji
 */
public final class TurnHandle {
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final CompletableFuture<TurnResult> completion = new CompletableFuture<>();
    private final Set<Runnable> cancelHooks = ConcurrentHashMap.newKeySet();
    private volatile Runnable cancelAction = () -> {};

    public void cancel() {
        cancelled.set(true);
        cancelAction.run();
        cancelHooks.forEach(Runnable::run);
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

    void onCancel(Runnable action) {
        if (cancelled.get()) {
            action.run();
        } else {
            cancelHooks.add(action);
        }
    }

    void removeOnCancel(Runnable action) {
        cancelHooks.remove(action);
    }

    void complete(TurnResult result) {
        completion.complete(result);
    }
}
