package com.izakji.mccreatorutils.locate;

import com.izakji.mccreatorutils.locate.engine.BiomeEnvelopeIndex;
import com.izakji.mccreatorutils.locate.engine.BiomeEnvelopeProvider;
import com.izakji.mccreatorutils.locate.engine.ParameterListEnvelopeProvider;
import com.izakji.mccreatorutils.mixin.MultiNoiseBiomeSourceAccessor;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.neoforged.fml.ModList;

/**
 * Selects the base or TerraBlender climate backend for a level.
 *
 * @author Izakji
 */
public final class BiomeBackendFactory {
    private static final Map<Climate.ParameterList<Holder<Biome>>, BiomeEnvelopeIndex<Biome>> BASE_CACHE =
            new IdentityHashMap<>();
    private static final BiomeEnvelopeProvider<Biome> EMPTY_PROVIDER = targets -> (quartX, quartZ) -> Optional.empty();

    private BiomeBackendFactory() {}

    public static BiomeBackend create(ServerLevel level) {
        BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
        Climate.Sampler sampler = level.getChunkSource().randomState().sampler();
        if (source instanceof MultiNoiseBiomeSource multi) {
            Climate.ParameterList<Holder<Biome>> parameters =
                    ((MultiNoiseBiomeSourceAccessor) multi).invokeParameters();
            BiomeEnvelopeProvider<Biome> base = baseProvider(parameters);
            if (ModList.get().isLoaded("terrablender") && TerraBlenderCompat.isAvailable(parameters, level)) {
                return new BiomeBackend(
                        TerraBlenderCompat.provider(parameters, level),
                        new MinecraftClimateSampler(
                                sampler, (point, x, y, z) -> TerraBlenderCompat.resolve(parameters, point, x, y, z)));
            }
            return new BiomeBackend(
                    base, new MinecraftClimateSampler(sampler, (point, x, y, z) -> multi.getNoiseBiome(point)));
        }

        return new BiomeBackend(
                EMPTY_PROVIDER,
                new MinecraftClimateSampler(
                        sampler,
                        (point, x, y, z) -> source.getNoiseBiome(
                                QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), sampler)));
    }

    public static void clearCache() {
        synchronized (BASE_CACHE) {
            BASE_CACHE.clear();
        }
        if (ModList.get().isLoaded("terrablender")) {
            TerraBlenderCompat.clearCache();
        }
    }

    private static BiomeEnvelopeProvider<Biome> baseProvider(Climate.ParameterList<Holder<Biome>> parameters) {
        synchronized (BASE_CACHE) {
            BiomeEnvelopeIndex<Biome> index = BASE_CACHE.computeIfAbsent(parameters, BiomeEnvelopeIndex::from);
            return new ParameterListEnvelopeProvider<>(index);
        }
    }
}
