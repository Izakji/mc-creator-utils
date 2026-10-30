package com.izakji.mccreatorutils.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class SafeCloseTest {

    @Test
    void runsActionWhenItSucceeds() {
        AtomicBoolean ran = new AtomicBoolean();
        SafeClose.run(() -> ran.set(true));
        assertTrue(ran.get());
    }

    @Test
    void swallowsRuntimeException() {
        assertDoesNotThrow(() -> SafeClose.run(() -> {
            throw new IllegalStateException("close failed");
        }));
    }

    @Test
    void swallowsLinkageError() {
        assertDoesNotThrow(() -> SafeClose.run(() -> {
            throw new NoClassDefFoundError("browser classes are absent");
        }));
    }

    @Test
    void doesNotSwallowFatalErrors() {
        assertThrows(
                AssertionError.class,
                () -> SafeClose.run(() -> {
                    throw new AssertionError("programming error");
                }));
        assertThrows(
                OutOfMemoryError.class,
                () -> SafeClose.run(() -> {
                    throw new OutOfMemoryError("out of memory");
                }));
    }
}
