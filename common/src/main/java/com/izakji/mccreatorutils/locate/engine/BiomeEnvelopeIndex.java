package com.izakji.mccreatorutils.locate.engine;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Climate;

/**
 * Caches one climate envelope per biome for a parameter list.
 *
 * @author Izakji
 */
public final class BiomeEnvelopeIndex<T> {
    private final Map<Holder<T>, BiomeEnvelope> envelopes;
    private final CoverageIndex coverage;

    private BiomeEnvelopeIndex(Map<Holder<T>, BiomeEnvelope> envelopes, CoverageIndex coverage) {
        this.envelopes = Map.copyOf(envelopes);
        this.coverage = coverage;
    }

    public static <T> BiomeEnvelopeIndex<T> from(Climate.ParameterList<Holder<T>> parameters) {
        return fromValues(parameters.values());
    }

    public static <T> BiomeEnvelopeIndex<T> fromValues(List<Pair<Climate.ParameterPoint, Holder<T>>> values) {
        Map<Holder<T>, List<Climate.ParameterPoint>> pointsByTarget = new HashMap<>();
        List<Climate.ParameterPoint> points = new ArrayList<>(values.size());
        for (Pair<Climate.ParameterPoint, Holder<T>> pair : values) {
            pointsByTarget
                    .computeIfAbsent(pair.getSecond(), ignored -> new ArrayList<>())
                    .add(pair.getFirst());
            points.add(pair.getFirst());
        }

        Map<Holder<T>, BiomeEnvelope> built = new HashMap<>();
        for (Map.Entry<Holder<T>, List<Climate.ParameterPoint>> entry : pointsByTarget.entrySet()) {
            built.put(entry.getKey(), BiomeEnvelope.of(entry.getValue()));
        }
        return new BiomeEnvelopeIndex<>(built, CoverageIndex.fromPoints(points));
    }

    public Optional<BiomeEnvelope> envelopeFor(Holder<T> target) {
        return Optional.ofNullable(envelopes.get(target));
    }

    public Optional<BiomeEnvelope> unionFor(Set<Holder<T>> targets) {
        if (targets.isEmpty()) {
            return Optional.empty();
        }

        BiomeEnvelope union = BiomeEnvelope.impossible();
        for (Holder<T> target : targets) {
            BiomeEnvelope envelope = envelopes.get(target);
            if (envelope == null) {
                return Optional.empty();
            }
            union = BiomeEnvelope.union(union, envelope);
        }
        return Optional.of(union);
    }

    public BiomeEnvelope unionForPresent(Set<Holder<T>> targets) {
        BiomeEnvelope union = BiomeEnvelope.impossible();
        for (Holder<T> target : targets) {
            BiomeEnvelope envelope = envelopes.get(target);
            if (envelope != null) {
                union = BiomeEnvelope.union(union, envelope);
            }
        }
        return union;
    }

    public boolean covers(Climate.TargetPoint point) {
        return coverage.covers(point);
    }

    public boolean coversFive(long temperature, long humidity, long continentalness, long erosion, long weirdness) {
        return coverage.coversFive(temperature, humidity, continentalness, erosion, weirdness);
    }
}
