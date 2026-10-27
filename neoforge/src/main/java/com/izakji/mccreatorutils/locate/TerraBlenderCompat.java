package com.izakji.mccreatorutils.locate;

import com.izakji.mccreatorutils.locate.engine.BiomeEnvelope;
import com.izakji.mccreatorutils.locate.engine.BiomeEnvelopeIndex;
import com.izakji.mccreatorutils.locate.engine.BiomeEnvelopeProvider;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;
import terrablender.api.Regions;
import terrablender.util.LevelUtils;
import terrablender.worldgen.IExtendedParameterList;

/**
 * Optional TerraBlender region-envelope adapter.
 *
 * <p>This class must only be loaded after confirming that TerraBlender is present at runtime.</p>
 *
 * @author Izakji
 */
public final class TerraBlenderCompat {
    private static final Map<Climate.ParameterList<Holder<Biome>>, Map<RegionType, BiomeEnvelopeProvider<Biome>>>
            CACHE = new IdentityHashMap<>();

    private TerraBlenderCompat() {}

    public static boolean isAvailable(Climate.ParameterList<Holder<Biome>> parameters, ServerLevel level) {
        if (!(parameters instanceof IExtendedParameterList<?> extended) || !extended.isInitialized()) {
            return false;
        }
        return LevelUtils.getRegionTypeForDimension(level.dimensionTypeRegistration()) != null;
    }

    public static BiomeEnvelopeProvider<Biome> provider(
            Climate.ParameterList<Holder<Biome>> parameters, ServerLevel level) {
        RegionType regionType = LevelUtils.getRegionTypeForDimension(level.dimensionTypeRegistration());
        synchronized (CACHE) {
            Map<RegionType, BiomeEnvelopeProvider<Biome>> byType =
                    CACHE.computeIfAbsent(parameters, ignored -> new HashMap<>());
            return byType.computeIfAbsent(regionType, ignored -> build(parameters, level, regionType));
        }
    }

    public static Holder<Biome> resolve(
            Climate.ParameterList<Holder<Biome>> parameters,
            Climate.TargetPoint point,
            int blockX,
            int blockY,
            int blockZ) {
        return ((IExtendedParameterList<Holder<Biome>>) parameters)
                .findValuePositional(
                        point, QuartPos.fromBlock(blockX), QuartPos.fromBlock(blockY), QuartPos.fromBlock(blockZ));
    }

    public static void clearCache() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    private static BiomeEnvelopeProvider<Biome> build(
            Climate.ParameterList<Holder<Biome>> parameters, ServerLevel level, RegionType regionType) {
        BiomeEnvelopeIndex<Biome> base = BiomeEnvelopeIndex.from(parameters);
        Registry<Biome> biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);
        Map<Integer, BiomeEnvelopeIndex<Biome>> regionIndexes = new HashMap<>();
        regionIndexes.put(0, base);

        for (Region region : Regions.get(regionType)) {
            int index = Regions.getIndex(regionType, region.getName());
            if (index == 0) {
                continue;
            }

            List<Pair<Climate.ParameterPoint, Holder<Biome>>> values = new ArrayList<>();
            region.addBiomes(
                    biomeRegistry,
                    pair -> biomeRegistry
                            .getHolder(pair.getSecond())
                            .ifPresent(holder -> values.add(Pair.of(pair.getFirst(), holder))));
            regionIndexes.put(index, BiomeEnvelopeIndex.fromValues(values));
        }

        IExtendedParameterList<Holder<Biome>> extended = (IExtendedParameterList<Holder<Biome>>) parameters;
        return targets -> {
            if (targets.isEmpty()) {
                return (quartX, quartZ) -> Optional.empty();
            }

            for (Holder<Biome> target : targets) {
                if (base.envelopeFor(target).isPresent()) {
                    continue;
                }
                boolean represented = regionIndexes.values().stream()
                        .anyMatch(index -> index.envelopeFor(target).isPresent());
                if (!represented) {
                    return (quartX, quartZ) -> Optional.empty();
                }
            }

            Map<Integer, BiomeEnvelope> envelopes = new HashMap<>();
            for (Map.Entry<Integer, BiomeEnvelopeIndex<Biome>> entry : regionIndexes.entrySet()) {
                BiomeEnvelope basePart = base.unionForPresent(targets);
                BiomeEnvelope regionPart = entry.getValue().unionForPresent(targets);
                envelopes.put(entry.getKey(), BiomeEnvelope.union(basePart, regionPart));
            }

            return (quartX, quartZ) -> {
                int uniqueness = extended.getUniqueness(quartX, 0, quartZ);
                BiomeEnvelope envelope = envelopes.get(uniqueness);
                return envelope == null ? Optional.empty() : Optional.of(envelope);
            };
        };
    }
}
