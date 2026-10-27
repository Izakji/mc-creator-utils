package com.izakji.mccreatorutils.locate;

import com.izakji.mccreatorutils.locate.engine.BiomeEnvelopeProvider;
import com.izakji.mccreatorutils.locate.engine.ClimateSampler;
import net.minecraft.world.level.biome.Biome;

/**
 * Minecraft-backed provider and sampler pair for one level.
 *
 * @author Izakji
 */
public record BiomeBackend(BiomeEnvelopeProvider<Biome> envelopes, ClimateSampler<Biome> sampler) {}
