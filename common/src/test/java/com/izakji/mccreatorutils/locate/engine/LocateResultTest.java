package com.izakji.mccreatorutils.locate.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import org.junit.jupiter.api.Test;

class LocateResultTest {

    @Test
    void reportsSuccessAndFailures() {
        var holder = Holder.<String>direct("plains");
        var found = LocateResult.found(BlockPos.ZERO, holder, 12);

        assertTrue(found.success());
        assertEquals(BlockPos.ZERO, found.position());
        assertEquals(holder, found.holder());
        assertEquals(12, found.elapsedMillis());

        assertFalse(LocateResult.notFound(4).success());
        assertEquals(LocateFailure.NOT_FOUND, LocateResult.notFound(4).failure());
        assertFalse(LocateResult.failed(LocateFailure.TIMEOUT, 9).success());
    }
}
