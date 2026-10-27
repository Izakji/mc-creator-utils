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

    private BiomeEnvelopeIndex(Map<Holder<T>, BiomeEnvelope> envelopes) {
        this.envelopes = Map.copyOf(envelopes);
    }

    public static <T> BiomeEnvelopeIndex<T> from(Climate.ParameterList<Holder<T>> parameters) {
        return fromValues(parameters.values());
    }

    public static <T> BiomeEnvelopeIndex<T> fromValues(List<Pair<Climate.ParameterPoint, Holder<T>>> values) {
        Map<Holder<T>, List<Climate.ParameterPoint>> pointsByTarget = new HashMap<>();
        for (Pair<Climate.ParameterPoint, Holder<T>> pair : values) {
            pointsByTarget
                    .computeIfAbsent(pair.getSecond(), ignored -> new ArrayList<>())
                    .add(pair.getFirst());
        }

        Map<Holder<T>, BiomeEnvelope> built = new HashMap<>();
        for (Map.Entry<Holder<T>, List<Climate.ParameterPoint>> entry : pointsByTarget.entrySet()) {
            built.put(entry.getKey(), BiomeEnvelope.of(entry.getValue()));
        }
        return new BiomeEnvelopeIndex<>(built);
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
}
