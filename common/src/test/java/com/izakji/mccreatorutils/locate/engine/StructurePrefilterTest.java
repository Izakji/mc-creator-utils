package com.izakji.mccreatorutils.locate.engine;

import static com.izakji.mccreatorutils.locate.engine.TestClimate.envelope;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.junit.jupiter.api.Test;

class StructurePrefilterTest {

    @Test
    void skipsChunkWhenEverySampleIsOutsideEnvelope() {
        var target = Holder.<String>direct("target");
        var sampler = new TestClimateSampler();
        sampler.fillValues(1000);
        var filter =
                new StructurePrefilter<>(coveredProvider(envelope(0, 100)), sampler, structure -> Set.of(target), 0);

        assertFalse(filter.mayMatch(Holder.<Structure>direct(null), new ChunkPos(0, 0)));
    }

    @Test
    void marginFindsBiomeInAdjacentChunk() {
        var target = Holder.<String>direct("target");
        var sampler = new TestClimateSampler();
        sampler.fillValues(1000);
        sampler.putValues(-16, 0, 50, 50, 50, 50, 50);
        var noMargin =
                new StructurePrefilter<>(coveredProvider(envelope(0, 100)), sampler, structure -> Set.of(target), 0);
        var withMargin =
                new StructurePrefilter<>(coveredProvider(envelope(0, 100)), sampler, structure -> Set.of(target), 1);

        assertFalse(noMargin.mayMatch(Holder.<Structure>direct(null), new ChunkPos(0, 0)));
        assertTrue(withMargin.mayMatch(Holder.<Structure>direct(null), new ChunkPos(0, 0)));
    }

    @Test
    void uncoveredSampleOutsideEnvelopeDoesNotPrune() {
        var target = Holder.<String>direct("target");
        var sampler = new TestClimateSampler();
        sampler.fillValues(1000);
        var filter = new StructurePrefilter<>(
                targets -> (qx, qz) -> Optional.of(envelope(0, 100)), sampler, structure -> Set.of(target), 0);

        assertTrue(filter.mayMatch(Holder.<Structure>direct(null), new ChunkPos(0, 0)));
    }

    @Test
    void missingEnvelopeOrBiomesDisablesFiltering() {
        var target = Holder.<String>direct("target");
        var sampler = new TestClimateSampler();
        sampler.fillValues(1000);
        var noEnvelope = new StructurePrefilter<String>(
                targets -> (qx, qz) -> Optional.empty(), sampler, structure -> Set.of(target), 0);
        var noBiomes = new StructurePrefilter<>(coveredProvider(envelope(0, 100)), sampler, structure -> Set.of(), 0);

        assertTrue(noEnvelope.mayMatch(Holder.<Structure>direct(null), new ChunkPos(0, 0)));
        assertTrue(noBiomes.mayMatch(Holder.<Structure>direct(null), new ChunkPos(0, 0)));
        assertTrue(StructurePrefilter.disabled().mayMatch(Holder.<Structure>direct(null), new ChunkPos(0, 0)));
    }

    private static BiomeEnvelopeProvider<String> coveredProvider(BiomeEnvelope envelope) {
        return targets -> new BiomeEnvelopeProvider.EnvelopeLookup<>() {
            @Override
            public Optional<BiomeEnvelope> at(int quartX, int quartZ) {
                return Optional.of(envelope);
            }

            @Override
            public boolean covers(Climate.TargetPoint point, int quartX, int quartZ) {
                return true;
            }

            @Override
            public boolean coversFive(
                    long temperature,
                    long humidity,
                    long continentalness,
                    long erosion,
                    long weirdness,
                    int quartX,
                    int quartZ) {
                return true;
            }
        };
    }
}
