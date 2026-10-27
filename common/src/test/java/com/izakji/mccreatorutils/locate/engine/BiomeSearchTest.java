package com.izakji.mccreatorutils.locate.engine;

import static com.izakji.mccreatorutils.locate.engine.TestClimate.point;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Climate;
import org.junit.jupiter.api.Test;

class BiomeSearchTest {
    private static final BlockPos ORIGIN = new BlockPos(0, 64, 0);
    private static final int MIN_Y = -64;
    private static final int MAX_Y = 320;

    @Test
    void findsFirstTargetAndSamplesEachAxisOncePerColumn() {
        var target = Holder.<String>direct("target");
        var sampler = new TestClimateSampler();
        sampler.putValues(0, 0, 100, 100, 100, 100, 100);
        sampler.putHit(0, 64, 0, target);

        var hit = BiomeSearch.findClosest(input(Set.of(target), enclosingEnvelope(), sampler))
                .orElseThrow();

        assertEquals(new BlockPos(0, 64, 0), hit.position());
        assertEquals(target, hit.biome());
        assertEquals(0, sampler.duplicateAxisSamples());
    }

    @Test
    void envelopeRejectsColumnBeforeResolve() {
        var target = Holder.<String>direct("target");
        var sampler = new TestClimateSampler();
        sampler.fillValues(1000);
        sampler.putValues(0, 0, 1000, 1000, 1000, 1000, 1000);
        sampler.putHit(0, 64, 0, target);

        var input = input(
                Set.of(target),
                coveringEnvelope(BiomeEnvelope.of(List.of(point(0, 100, 0, 100, 0, 100, 0, 100, 0, 100)))),
                sampler);

        assertTrue(BiomeSearch.findClosest(input).isEmpty());
        assertEquals(0, sampler.resolveCalls());
    }

    @Test
    void uncoveredSampleOutsideTargetEnvelopeFallsThroughToExactSearch() {
        var target = Holder.<String>direct("target");
        var sampler = new TestClimateSampler();
        sampler.putValues(0, 0, 110, 110, 110, 110, 110);
        sampler.putHit(0, 64, 0, target);

        var input = input(
                Set.of(target),
                targets -> (qx, qz) ->
                        Optional.of(BiomeEnvelope.of(List.of(point(0, 100, 0, 100, 0, 100, 0, 100, 0, 100)))),
                sampler);

        assertEquals(target, BiomeSearch.findClosest(input).orElseThrow().biome());
    }

    @Test
    void noEnvelopeFallsBackToExactSearch() {
        var target = Holder.<String>direct("target");
        var sampler = new TestClimateSampler();
        sampler.putValues(0, 0, 100, 100, 100, 100, 100);
        sampler.putHit(0, 64, 0, target);

        var input = input(Set.of(target), targets -> (qx, qz) -> Optional.empty(), sampler);

        assertEquals(target, BiomeSearch.findClosest(input).orElseThrow().biome());
    }

    @Test
    void emptyTargetsReturnWithoutSampling() {
        var sampler = new TestClimateSampler();
        var input = input(Set.of(), targets -> (qx, qz) -> Optional.empty(), sampler);

        assertTrue(BiomeSearch.findClosest(input).isEmpty());
        assertEquals(0, sampler.columnCalls());
    }

    @Test
    void cancellationStopsSearch() {
        var sampler = new TestClimateSampler();
        var input = input(
                Set.of(Holder.<String>direct("target")), targets -> (qx, qz) -> Optional.empty(), sampler, () -> true);

        assertThrows(CancellationException.class, () -> BiomeSearch.findClosest(input));
    }

    private static BiomeSearchInput<String> input(
            Set<Holder<String>> targets, BiomeEnvelopeProvider<String> provider, TestClimateSampler sampler) {
        return input(targets, provider, sampler, () -> false);
    }

    private static BiomeSearchInput<String> input(
            Set<Holder<String>> targets,
            BiomeEnvelopeProvider<String> provider,
            TestClimateSampler sampler,
            CancellationToken cancellationToken) {
        return new BiomeSearchInput<>(
                ORIGIN,
                64,
                32,
                32,
                MIN_Y,
                MAX_Y,
                targets,
                provider.forTargets(targets),
                sampler,
                new LocateStats(),
                cancellationToken);
    }

    private static BiomeEnvelopeProvider<String> enclosingEnvelope() {
        return targets -> (qx, qz) -> Optional.of(TestClimate.envelope(0, 200));
    }

    private static BiomeEnvelopeProvider<String> coveringEnvelope(BiomeEnvelope envelope) {
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
