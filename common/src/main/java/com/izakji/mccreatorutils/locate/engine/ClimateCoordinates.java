package com.izakji.mccreatorutils.locate.engine;

import net.minecraft.core.QuartPos;

/**
 * Quart-coordinate helpers for vanilla-compatible climate sampling.
 *
 * @author Izakji
 */
public final class ClimateCoordinates {
    private ClimateCoordinates() {}

    public static int alignToQuart(int blockCoord) {
        return QuartPos.toBlock(QuartPos.fromBlock(blockCoord));
    }
}
