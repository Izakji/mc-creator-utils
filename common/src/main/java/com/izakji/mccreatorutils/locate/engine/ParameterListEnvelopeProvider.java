package com.izakji.mccreatorutils.locate.engine;

import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Climate;

/**
 * Base parameter-list envelope provider.
 *
 * @param <T> biome value type
 * @author Izakji
 */
public final class ParameterListEnvelopeProvider<T> implements BiomeEnvelopeProvider<T> {
    private final BiomeEnvelopeIndex<T> index;

    /**
     * Creates a provider backed by one envelope index.
     *
     * @param index envelope index
     */
    public ParameterListEnvelopeProvider(BiomeEnvelopeIndex<T> index) {
        this.index = index;
    }

    @Override
    public EnvelopeLookup<T> forTargets(Set<Holder<T>> targets) {
        Optional<BiomeEnvelope> envelope = index.unionFor(targets);
        if (envelope.isEmpty()) {
            return (quartX, quartZ) -> Optional.empty();
        }
        BiomeEnvelope value = envelope.get();
        return new EnvelopeLookup<>() {
            @Override
            public Optional<BiomeEnvelope> at(int quartX, int quartZ) {
                return Optional.of(value);
            }

            @Override
            public boolean covers(Climate.TargetPoint point, int quartX, int quartZ) {
                return index.covers(point);
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
                return index.coversFive(temperature, humidity, continentalness, erosion, weirdness);
            }
        };
    }
}
