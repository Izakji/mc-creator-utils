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
 * @param <T> biome value type
 * @author Izakji
 */
public final class BiomeEnvelopeIndex<T> {
    private final Map<Holder<T>, BiomeEnvelope> envelopes;
    private final CoverageIndex coverage;

    private BiomeEnvelopeIndex(Map<Holder<T>, BiomeEnvelope> envelopes, CoverageIndex coverage) {
        this.envelopes = Map.copyOf(envelopes);
        this.coverage = coverage;
    }

    /**
     * Builds an index from a parameter list.
     *
     * @param parameters climate parameter list
     * @param <T> biome value type
     * @return envelope index
     */
    public static <T> BiomeEnvelopeIndex<T> from(Climate.ParameterList<Holder<T>> parameters) {
        return fromValues(parameters.values());
    }

    /**
     * Builds an index from parameter-point pairs.
     *
     * @param values parameter-point pairs
     * @param <T> biome value type
     * @return envelope index
     */
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

    /**
     * Returns the envelope for one biome.
     *
     * @param target biome holder
     * @return envelope, or empty when the biome has no parameter points
     */
    public Optional<BiomeEnvelope> envelopeFor(Holder<T> target) {
        return Optional.ofNullable(envelopes.get(target));
    }

    /**
     * Returns the union envelope, requiring every target to be represented.
     *
     * @param targets biome targets
     * @return union envelope, or empty when a target is missing
     */
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

    /**
     * Returns the union of the targets that have envelopes.
     *
     * @param targets biome targets
     * @return union envelope, or an impossible envelope when none are present
     */
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

    /**
     * Returns whether pruning is safe at a target point.
     *
     * @param point target point
     * @return true when a zero-offset non-target point contains the sample
     */
    public boolean covers(Climate.TargetPoint point) {
        return coverage.covers(point);
    }

    /**
     * Returns whether pruning is safe for the five height-independent axes.
     *
     * @param temperature temperature
     * @param humidity humidity
     * @param continentalness continentalness
     * @param erosion erosion
     * @param weirdness weirdness
     * @return true when a zero-offset non-target point contains the sample
     */
    public boolean coversFive(long temperature, long humidity, long continentalness, long erosion, long weirdness) {
        return coverage.coversFive(temperature, humidity, continentalness, erosion, weirdness);
    }
}
