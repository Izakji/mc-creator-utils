package com.izakji.mccreatorutils.locate.engine;

import static com.izakji.mccreatorutils.locate.engine.TestClimate.point;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Climate;
import org.junit.jupiter.api.Test;

class BiomeEnvelopeIndexTest {

    @Test
    void groupsTargetsAndDisablesPruningForMissingTargets() {
        var plains = Holder.<String>direct("plains");
        var forest = Holder.<String>direct("forest");
        var parameters = new Climate.ParameterList<>(List.of(
                Pair.of(point(0, 100, 0, 100, 0, 100, 0, 100, 0, 100), plains),
                Pair.of(point(0, 100, 0, 100, 0, 100, 0, 100, 0, 100), forest)));
        var provider = new ParameterListEnvelopeProvider<>(BiomeEnvelopeIndex.from(parameters));

        assertTrue(provider.forTargets(Set.of(plains)).at(0, 0).isPresent());
        assertTrue(provider.forTargets(Set.of(plains, Holder.<String>direct("missing")))
                .at(0, 0)
                .isEmpty());
    }

    @Test
    void coverageRequiresAParameterPointToContainTheSample() {
        var target = Holder.<String>direct("target");
        var index = BiomeEnvelopeIndex.fromValues(List.of(Pair.of(point(0, 10, 0, 10, 0, 10, 0, 10, 0, 10), target)));

        assertTrue(index.coversFive(5, 5, 5, 5, 5));
        assertFalse(index.coversFive(11, 5, 5, 5, 5));
        assertTrue(index.covers(new Climate.TargetPoint(5, 5, 5, 5, 0, 5)));
        assertFalse(index.covers(new Climate.TargetPoint(11, 5, 5, 5, 0, 5)));
    }

    @Test
    void coverageRequiresAZeroOffsetParameterPoint() {
        var target = Holder.<String>direct("target");
        var ordered = new Climate.ParameterPoint(
                new Climate.Parameter(0, 10),
                new Climate.Parameter(0, 10),
                new Climate.Parameter(0, 10),
                new Climate.Parameter(0, 10),
                new Climate.Parameter(-100, 100),
                new Climate.Parameter(0, 10),
                5L);
        var index = BiomeEnvelopeIndex.fromValues(List.of(Pair.of(ordered, target)));

        assertFalse(index.covers(new Climate.TargetPoint(5, 5, 5, 5, 0, 5)));
        assertFalse(index.coversFive(5, 5, 5, 5, 5));
    }
}
