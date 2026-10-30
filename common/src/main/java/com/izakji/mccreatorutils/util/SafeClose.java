package com.izakji.mccreatorutils.util;

import com.izakji.mccreatorutils.McCreatorUtils;

/**
 * Runs cleanup actions without allowing recoverable failures to escape.
 *
 * @author Izakji
 */
public final class SafeClose {
    private SafeClose() {}

    /**
     * Runs a close action, logging recoverable failures so screen teardown can continue.
     *
     * <p>Runtime exceptions and linkage errors are contained because a browser or optional
     * dependency can fail while unloading. Fatal errors such as {@link OutOfMemoryError} and
     * {@link AssertionError} propagate, since continuing after them can leave corrupt state.
     *
     * @param action close action
     */
    public static void run(Runnable action) {
        if (action == null) {
            return;
        }
        try {
            action.run();
        } catch (RuntimeException | LinkageError failure) {
            McCreatorUtils.LOGGER.warn("Cleanup action failed", failure);
        }
    }
}
