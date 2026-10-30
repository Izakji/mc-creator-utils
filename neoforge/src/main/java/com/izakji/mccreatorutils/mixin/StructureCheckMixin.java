package com.izakji.mccreatorutils.mixin;

import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Map;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Synchronizes StructureCheck state between normal chunk generation and the structure lane.
 *
 * <p>The vanilla monitor covered the whole {@code checkStart} body, including region-file reads and
 * biome sampling. That made the server tick wait behind the locate lane. This mixin keeps the same
 * monitor but narrows it to the in-memory maps, leaving storage and worldgen work lock-free.
 *
 * @author Izakji
 */
@Mixin(StructureCheck.class)
public abstract class StructureCheckMixin {
    @Shadow
    @Final
    private Long2ObjectMap<Object2IntMap<Structure>> loadedChunks;

    @Shadow
    @Final
    private Map<Structure, Long2BooleanMap> featureChecks;

    @Shadow
    @Final
    private long seed;

    @Shadow
    private StructureCheckResult checkStructureInfo(
            Object2IntMap<Structure> structureChunks, Structure structure, boolean skipKnownStructures) {
        throw new AssertionError();
    }

    @Shadow
    private StructureCheckResult tryLoadFromStorage(
            ChunkPos chunkPos, Structure structure, boolean skipKnownStructures, long packedChunkPos) {
        throw new AssertionError();
    }

    @Shadow
    private boolean canCreateStructure(ChunkPos chunkPos, Structure structure) {
        throw new AssertionError();
    }

    @Shadow
    private void storeFullResults(long chunkPos, Object2IntMap<Structure> structureChunks) {
        throw new AssertionError();
    }

    @Inject(method = "checkStart", at = @At("HEAD"), cancellable = true)
    private void mccreatorutils$checkStartWithoutIoLock(
            ChunkPos chunkPos,
            Structure structure,
            StructurePlacement placement,
            boolean skipKnownStructures,
            CallbackInfoReturnable<StructureCheckResult> cir) {
        long packedChunkPos = chunkPos.toLong();
        synchronized (this) {
            Object2IntMap<Structure> stored = loadedChunks.get(packedChunkPos);
            if (stored != null) {
                cir.setReturnValue(checkStructureInfo(stored, structure, skipKnownStructures));
                return;
            }
        }

        StructureCheckResult fromStorage = tryLoadFromStorage(chunkPos, structure, skipKnownStructures, packedChunkPos);
        if (fromStorage != null) {
            cir.setReturnValue(fromStorage);
            return;
        }
        if (!placement.applyAdditionalChunkRestrictions(chunkPos.x, chunkPos.z, seed)) {
            cir.setReturnValue(StructureCheckResult.START_NOT_PRESENT);
            return;
        }

        boolean known = false;
        boolean canCreate = false;
        synchronized (this) {
            Long2BooleanMap checks = featureChecks.get(structure);
            if (checks != null && checks.containsKey(packedChunkPos)) {
                known = true;
                canCreate = checks.get(packedChunkPos);
            }
        }
        if (!known) {
            boolean computed = canCreateStructure(chunkPos, structure);
            synchronized (this) {
                Long2BooleanMap checks =
                        featureChecks.computeIfAbsent(structure, ignored -> new Long2BooleanOpenHashMap());
                if (checks.containsKey(packedChunkPos)) {
                    canCreate = checks.get(packedChunkPos);
                } else {
                    checks.put(packedChunkPos, computed);
                    canCreate = computed;
                }
            }
        }
        cir.setReturnValue(canCreate ? StructureCheckResult.CHUNK_LOAD_NEEDED : StructureCheckResult.START_NOT_PRESENT);
    }

    @Inject(method = "storeFullResults", at = @At("HEAD"), cancellable = true)
    private void mccreatorutils$syncStoreFullResults(
            long chunkPos, Object2IntMap<Structure> structureChunks, CallbackInfo ci) {
        if (!Thread.holdsLock(this)) {
            synchronized (this) {
                storeFullResults(chunkPos, structureChunks);
            }
            ci.cancel();
        }
    }

    @Inject(method = "onStructureLoad", at = @At("HEAD"), cancellable = true)
    private void mccreatorutils$syncOnStructureLoad(
            ChunkPos chunkPos, Map<Structure, StructureStart> structureStarts, CallbackInfo ci) {
        if (!Thread.holdsLock(this)) {
            synchronized (this) {
                ((StructureCheck) (Object) this).onStructureLoad(chunkPos, structureStarts);
            }
            ci.cancel();
        }
    }

    @Inject(method = "incrementReference", at = @At("HEAD"), cancellable = true)
    private void mccreatorutils$syncIncrementReference(ChunkPos chunkPos, Structure structure, CallbackInfo ci) {
        if (!Thread.holdsLock(this)) {
            synchronized (this) {
                ((StructureCheck) (Object) this).incrementReference(chunkPos, structure);
            }
            ci.cancel();
        }
    }
}
