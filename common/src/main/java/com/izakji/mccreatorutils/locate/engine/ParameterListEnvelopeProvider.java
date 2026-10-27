package com.izakji.mccreatorutils.locate.engine;

import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;

/**
 * Base parameter-list envelope provider.
 *
 * @author Izakji
 */
public final class ParameterListEnvelopeProvider<T> implements BiomeEnvelopeProvider<T> {
    private final BiomeEnvelopeIndex<T> index;

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
        return (quartX, quartZ) -> Optional.of(value);
    }
}
