package com.izakji.mccreatorutils.locate.engine;

import static com.izakji.mccreatorutils.locate.engine.TestClimate.point;
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
}
