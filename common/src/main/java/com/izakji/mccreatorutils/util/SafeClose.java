package com.izakji.mccreatorutils.util;

/**
 * Runs cleanup actions without allowing failures to escape.
 *
 * @author Izakji
 */
public final class SafeClose {
    private SafeClose() {}

    /**
     * Runs a close action, swallowing runtime failures so screen teardown can continue.
     *
     * @param action close action
     */
    public static void run(Runnable action) {
        if (action == null) {
            return;
        }
        try {
            action.run();
        } catch (RuntimeException | Error ignored) {
            // Cleanup must not strand the screen or the game.
        }
    }
}
