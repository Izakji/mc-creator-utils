package com.izakji.mccreatorutils.locate;

import com.izakji.mccreatorutils.McCreatorUtils;
import com.izakji.mccreatorutils.locate.engine.BiomeHit;
import com.izakji.mccreatorutils.locate.engine.BiomeSearch;
import com.izakji.mccreatorutils.locate.engine.BiomeSearchInput;
import com.izakji.mccreatorutils.locate.engine.LocateFailure;
import com.izakji.mccreatorutils.locate.engine.LocateRequestKey;
import com.izakji.mccreatorutils.locate.engine.LocateResult;
import com.izakji.mccreatorutils.locate.engine.LocateService;
import com.izakji.mccreatorutils.locate.engine.LocateSettings;
import com.izakji.mccreatorutils.locate.engine.LocateStats;
import com.izakji.mccreatorutils.locate.engine.LocateType;
import com.izakji.mccreatorutils.locate.engine.StructureBiomeFilter;
import com.izakji.mccreatorutils.locate.engine.StructureHit;
import com.izakji.mccreatorutils.locate.engine.StructurePlacementGroup;
import com.izakji.mccreatorutils.locate.engine.StructurePrefilter;
import com.izakji.mccreatorutils.locate.engine.StructureSearch;
import com.izakji.mccreatorutils.locate.engine.StructureSearchInput;
import com.mojang.datafixers.util.Pair;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.LocateCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/**
 * Builds engine requests from command arguments and dispatches vanilla feedback.
 *
 * @author Izakji
 */
public final class LocateCoordinator {
    private final Map<UUID, Set<CompletableFuture<LocateResult>>> pending = new ConcurrentHashMap<>();

    public OptionalInt submitBiome(CommandSourceStack source, ResourceOrTagArgument.Result<Biome> target) {
        LocateService service = LocateLifecycle.service();
        LocateSettings settings = LocateConfig.settings();
        if (service == null || !settings.enabled()) {
            return OptionalInt.empty();
        }

        ServerLevel level = source.getLevel();
        Set<Holder<Biome>> targets = level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes().stream()
                .filter(target)
                .collect(Collectors.toUnmodifiableSet());
        if (targets.isEmpty()) {
            source.sendFailure(Component.translatableEscape("commands.locate.biome.not_found", target.asPrintable()));
            return OptionalInt.of(1);
        }

        BlockPos origin = BlockPos.containing(source.getPosition());
        LocateRequestKey key =
                new LocateRequestKey(level.dimension(), LocateType.BIOME, targetIds(targets), origin, 6400, false);
        LocateStats stats = new LocateStats();
        long started = System.nanoTime();
        CompletableFuture<LocateResult> future = track(
                source.getPlayer(),
                service.submit(
                        key,
                        token -> {
                            BiomeBackend backend = BiomeBackendFactory.create(level);
                            return BiomeSearch.findClosest(new BiomeSearchInput<>(
                                            origin,
                                            6400,
                                            32,
                                            64,
                                            level.getMinBuildHeight(),
                                            level.getMaxBuildHeight(),
                                            targets,
                                            backend.envelopes().forTargets(targets),
                                            backend.sampler(),
                                            stats,
                                            token))
                                    .map(hit -> foundBiome(hit, started))
                                    .orElseGet(() -> LocateResult.notFound(elapsed(started)));
                        },
                        settings.timeoutSeconds() * 1000L));
        future.thenAccept(result ->
                dispatch(source, level, () -> feedbackBiome(source, level, target, origin, result, stats, settings)));
        return OptionalInt.of(1);
    }

    public OptionalInt submitStructure(
            CommandSourceStack source,
            HolderSet<Structure> targets,
            ResourceOrTagKeyArgument.Result<Structure> printable) {
        LocateService service = LocateLifecycle.service();
        LocateSettings settings = LocateConfig.settings();
        if (service == null || !settings.enabled()) {
            return OptionalInt.empty();
        }

        ServerLevel level = source.getLevel();
        List<Holder<Structure>> holders = targets.stream().toList();
        if (holders.isEmpty()) {
            source.sendFailure(
                    Component.translatableEscape("commands.locate.structure.not_found", printable.asPrintable()));
            return OptionalInt.of(1);
        }

        BlockPos origin = BlockPos.containing(source.getPosition());
        ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
        LinkedHashMap<StructurePlacement, Set<Holder<Structure>>> placementMap = new LinkedHashMap<>();
        for (Holder<Structure> holder : holders) {
            for (StructurePlacement placement : state.getPlacementsForStructure(holder)) {
                placementMap
                        .computeIfAbsent(placement, ignored -> new java.util.LinkedHashSet<>())
                        .add(holder);
            }
        }

        List<StructurePlacementGroup> groups = new ArrayList<>();
        Map<ConcentricRingsStructurePlacement, List<ChunkPos>> concentricPositions = new LinkedHashMap<>();
        for (Map.Entry<StructurePlacement, Set<Holder<Structure>>> entry : placementMap.entrySet()) {
            groups.add(new StructurePlacementGroup(entry.getKey(), Set.copyOf(entry.getValue())));
            if (entry.getKey() instanceof ConcentricRingsStructurePlacement concentric) {
                List<ChunkPos> rings = state.getRingPositionsFor(concentric);
                concentricPositions.put(concentric, rings == null ? List.of() : List.copyOf(rings));
            }
        }

        LocateRequestKey key =
                new LocateRequestKey(level.dimension(), LocateType.STRUCTURE, targetIds(holders), origin, 100, false);
        LocateStats stats = new LocateStats();
        long started = System.nanoTime();
        CompletableFuture<LocateResult> future = track(
                source.getPlayer(),
                service.submit(
                        key,
                        token -> {
                            BiomeBackend backend = BiomeBackendFactory.create(level);
                            StructureBiomeFilter filter = settings.structurePrefilter()
                                    ? new StructurePrefilter<>(
                                            backend.envelopes(),
                                            backend.sampler(),
                                            holder -> holder.value().biomes().stream()
                                                    .collect(Collectors.toUnmodifiableSet()),
                                            settings.structurePrefilterMarginChunks())
                                    : StructurePrefilter.disabled();
                            return StructureSearch.findNearest(new StructureSearchInput(
                                            origin,
                                            100,
                                            state.getLevelSeed(),
                                            false,
                                            groups,
                                            concentricPositions,
                                            new StructureProbeImpl(level, filter, stats),
                                            stats,
                                            token))
                                    .map(hit -> foundStructure(hit, started))
                                    .orElseGet(() -> LocateResult.notFound(elapsed(started)));
                        },
                        settings.timeoutSeconds() * 1000L));
        future.thenAccept(result -> dispatch(
                source, level, () -> feedbackStructure(source, level, printable, origin, result, stats, settings)));
        return OptionalInt.of(1);
    }

    public void cancelPlayer(UUID playerId) {
        Set<CompletableFuture<LocateResult>> futures = pending.remove(playerId);
        if (futures == null) {
            return;
        }
        for (CompletableFuture<LocateResult> future : futures) {
            future.cancel(true);
        }
    }

    private CompletableFuture<LocateResult> track(ServerPlayer player, CompletableFuture<LocateResult> future) {
        if (player == null) {
            return future;
        }
        UUID playerId = player.getUUID();
        Set<CompletableFuture<LocateResult>> futures =
                pending.computeIfAbsent(playerId, ignored -> ConcurrentHashMap.newKeySet());
        futures.add(future);
        future.whenComplete((result, throwable) -> {
            futures.remove(future);
            pending.computeIfPresent(playerId, (ignored, current) -> current.isEmpty() ? null : current);
        });
        return future;
    }

    private static LocateResult foundBiome(BiomeHit<Biome> hit, long started) {
        return LocateResult.found(hit.position(), hit.biome(), elapsed(started));
    }

    private static LocateResult foundStructure(StructureHit hit, long started) {
        return LocateResult.found(hit.position(), hit.structure(), elapsed(started));
    }

    private static void dispatch(CommandSourceStack source, ServerLevel level, Runnable feedback) {
        MinecraftServer server = source.getServer();
        if (!server.isRunning()) {
            return;
        }
        server.execute(() -> {
            if (isValid(source, level)) {
                feedback.run();
            }
        });
    }

    private static boolean isValid(CommandSourceStack source, ServerLevel level) {
        MinecraftServer server = source.getServer();
        if (!server.isRunning() || server.getLevel(level.dimension()) != level) {
            return false;
        }
        ServerPlayer player = source.getPlayer();
        return player != null && server.getPlayerList().getPlayer(player.getUUID()) == player;
    }

    private static void feedbackBiome(
            CommandSourceStack source,
            ServerLevel level,
            ResourceOrTagArgument.Result<Biome> target,
            BlockPos origin,
            LocateResult result,
            LocateStats stats,
            LocateSettings settings) {
        debug(settings, "biome", target.asPrintable(), result, stats);
        if (result.success()) {
            LocateCommand.showLocateResult(
                    source,
                    target,
                    origin,
                    Pair.of(result.position(), result.holder()),
                    "commands.locate.biome.success",
                    true,
                    Duration.ofMillis(result.elapsedMillis()));
            return;
        }
        failure(source, result, target.asPrintable(), "commands.locate.biome.not_found");
    }

    private static void feedbackStructure(
            CommandSourceStack source,
            ServerLevel level,
            ResourceOrTagKeyArgument.Result<Structure> target,
            BlockPos origin,
            LocateResult result,
            LocateStats stats,
            LocateSettings settings) {
        debug(settings, "structure", target.asPrintable(), result, stats);
        if (result.success()) {
            LocateCommand.showLocateResult(
                    source,
                    target,
                    origin,
                    Pair.of(result.position(), result.holder()),
                    "commands.locate.structure.success",
                    false,
                    Duration.ofMillis(result.elapsedMillis()));
            return;
        }
        failure(source, result, target.asPrintable(), "commands.locate.structure.not_found");
    }

    private static void failure(CommandSourceStack source, LocateResult result, String target, String notFoundKey) {
        LocateFailure failure = result.failure();
        if (failure == null || failure == LocateFailure.CANCELLED) {
            return;
        }
        if (failure == LocateFailure.NOT_FOUND) {
            source.sendFailure(Component.translatableEscape(notFoundKey, target));
        } else if (failure == LocateFailure.QUEUE_FULL) {
            source.sendFailure(Component.translatable("commands.mccreatorutils.locate.queue_full"));
        } else if (failure == LocateFailure.TIMEOUT) {
            source.sendFailure(Component.translatable("commands.mccreatorutils.locate.timeout"));
        } else {
            source.sendFailure(Component.translatable("commands.mccreatorutils.locate.failed"));
        }
    }

    private static void debug(
            LocateSettings settings, String type, String target, LocateResult result, LocateStats stats) {
        if (settings.debugStats()) {
            McCreatorUtils.LOGGER.info(
                    "Locate {} {} -> {} in {}ms; {}",
                    type,
                    target,
                    result.failure() == null ? result.position() : result.failure(),
                    result.elapsedMillis(),
                    stats.summary());
        }
    }

    private static List<ResourceLocation> targetIds(Set<? extends Holder<?>> holders) {
        return holders.stream()
                .map(holder -> holder.unwrapKey().orElseThrow().location())
                .collect(Collectors.toUnmodifiableList());
    }

    private static List<ResourceLocation> targetIds(List<? extends Holder<?>> holders) {
        return holders.stream()
                .map(holder -> holder.unwrapKey().orElseThrow().location())
                .collect(Collectors.toUnmodifiableList());
    }

    private static long elapsed(long startedNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
    }
}
