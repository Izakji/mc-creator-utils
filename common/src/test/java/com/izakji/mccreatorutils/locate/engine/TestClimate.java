package com.izakji.mccreatorutils.locate.engine;

import java.util.List;
import net.minecraft.world.level.biome.Climate;

final class TestClimate {
    private TestClimate() {}

    static Climate.ParameterPoint point(
            int t0, int t1, int h0, int h1, int c0, int c1, int e0, int e1, int w0, int w1) {
        return new Climate.ParameterPoint(
                new Climate.Parameter(t0, t1),
                new Climate.Parameter(h0, h1),
                new Climate.Parameter(c0, c1),
                new Climate.Parameter(e0, e1),
                new Climate.Parameter(-100, 100),
                new Climate.Parameter(w0, w1),
                0L);
    }

    static BiomeEnvelope envelope(int min, int max) {
        return BiomeEnvelope.of(List.of(point(min, max, min, max, min, max, min, max, min, max)));
    }
}
