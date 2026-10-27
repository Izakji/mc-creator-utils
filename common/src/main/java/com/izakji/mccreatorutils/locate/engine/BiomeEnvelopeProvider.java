package com.izakji.mccreatorutils.locate.engine;

import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Climate;

/**
 * Supplies climate envelopes for a target set at a quart position.
 *
 * <p>An empty lookup means the provider cannot safely prune this target set. An
 * {@link Optional#empty()} envelope in a lookup also means "do not prune".</p>
 *
 * <p>The coverage methods decide whether the nearest-neighbour resolver must
 * return a non-target parameter point. Providers return true only when a
 * zero-offset non-target parameter point contains the sample (and, for the
 * five-axis form, when that conclusion holds for the sampled height-independent
 * axes). The default false disables pruning.</p>
 *
 * @author Izakji
 */
public interface BiomeEnvelopeProvider<T> {

    EnvelopeLookup<T> forTargets(Set<Holder<T>> targets);

    @FunctionalInterface
    interface EnvelopeLookup<T> {
        Optional<BiomeEnvelope> at(int quartX, int quartZ);

        default boolean covers(Climate.TargetPoint point, int quartX, int quartZ) {
            return false;
        }

        default boolean coversFive(
                long temperature,
                long humidity,
                long continentalness,
                long erosion,
                long weirdness,
                int quartX,
                int quartZ) {
            return false;
        }
    }
}
