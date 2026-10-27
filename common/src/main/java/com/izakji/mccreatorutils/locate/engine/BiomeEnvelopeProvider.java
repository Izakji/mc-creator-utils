package com.izakji.mccreatorutils.locate.engine;

import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;

/**
 * Supplies climate envelopes for a target set at a quart position.
 *
 * <p>An empty lookup means the provider cannot safely prune this target set. An
 * {@link Optional#empty()} envelope in a lookup also means "do not prune".</p>
 *
 * @author Izakji
 */
public interface BiomeEnvelopeProvider<T> {

    EnvelopeLookup<T> forTargets(Set<Holder<T>> targets);

    @FunctionalInterface
    interface EnvelopeLookup<T> {
        Optional<BiomeEnvelope> at(int quartX, int quartZ);
    }
}
