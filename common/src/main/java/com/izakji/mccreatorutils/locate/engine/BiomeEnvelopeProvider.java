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
 * @param <T> biome value type
 * @author Izakji
 */
public interface BiomeEnvelopeProvider<T> {

    /**
     * Returns a lookup for one target set.
     *
     * @param targets biome targets
     * @return envelope lookup
     */
    EnvelopeLookup<T> forTargets(Set<Holder<T>> targets);

    /** Per-target-set envelope and coverage lookup. */
    @FunctionalInterface
    interface EnvelopeLookup<T> {
        /**
         * Returns the envelope at a quart position.
         *
         * @param quartX quart x
         * @param quartZ quart z
         * @return envelope, or empty when pruning is unsafe
         */
        Optional<BiomeEnvelope> at(int quartX, int quartZ);

        /**
         * Returns whether pruning is safe at a target point.
         *
         * @param point target point
         * @param quartX quart x
         * @param quartZ quart z
         * @return true when a zero-offset non-target point contains the sample
         */
        default boolean covers(Climate.TargetPoint point, int quartX, int quartZ) {
            return false;
        }

        /**
         * Returns whether pruning is safe for the five height-independent axes.
         *
         * @param temperature temperature
         * @param humidity humidity
         * @param continentalness continentalness
         * @param erosion erosion
         * @param weirdness weirdness
         * @param quartX quart x
         * @param quartZ quart z
         * @return true when a zero-offset non-target point contains the sample
         */
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
