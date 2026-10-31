package com.izakji.mccreatorutils.client.agent.context;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;

/**
 * Runs client-only reads on the render thread and returns a future.
 *
 * @author Izakji
 */
final class ClientThread {
    private ClientThread() {}

    static <T> CompletableFuture<T> supply(Supplier<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Minecraft minecraft = Minecraft.getInstance();
        Runnable action = () -> {
            try {
                future.complete(task.get());
            } catch (Throwable failure) {
                future.completeExceptionally(failure);
            }
        };
        if (minecraft.isSameThread()) {
            action.run();
        } else {
            minecraft.execute(action);
        }
        return future;
    }
}
