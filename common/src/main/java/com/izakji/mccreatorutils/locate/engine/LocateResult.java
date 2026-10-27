package com.izakji.mccreatorutils.locate.engine;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;

/**
 * Result of an asynchronous locate request.
 *
 * @param position located position, or {@code null} on failure
 * @param holder matched registry holder, or {@code null} on failure
 * @param failure failure reason, or {@code null} on success
 * @param elapsedMillis elapsed request time in milliseconds
 * @author Izakji
 */
public record LocateResult(BlockPos position, Holder<?> holder, LocateFailure failure, long elapsedMillis) {

    public static LocateResult found(BlockPos position, Holder<?> holder, long elapsedMillis) {
        return new LocateResult(
                Objects.requireNonNull(position, "position"),
                Objects.requireNonNull(holder, "holder"),
                null,
                elapsedMillis);
    }

    public static LocateResult notFound(long elapsedMillis) {
        return new LocateResult(null, null, LocateFailure.NOT_FOUND, elapsedMillis);
    }

    public static LocateResult failed(LocateFailure failure, long elapsedMillis) {
        return new LocateResult(null, null, Objects.requireNonNull(failure, "failure"), elapsedMillis);
    }

    public boolean success() {
        return position != null && failure == null;
    }
}
