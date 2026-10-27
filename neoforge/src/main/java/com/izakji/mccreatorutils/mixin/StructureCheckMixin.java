package com.izakji.mccreatorutils.mixin;

import java.util.Map;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Synchronizes StructureCheck state between normal chunk generation and the structure lane.
 *
 * @author Izakji
 */
@Mixin(StructureCheck.class)
public abstract class StructureCheckMixin {

    @Inject(method = "checkStart", at = @At("HEAD"), cancellable = true)
    private void mccreatorutils$syncCheckStart(
            ChunkPos chunkPos,
            Structure structure,
            StructurePlacement placement,
            boolean skipKnownStructures,
            CallbackInfoReturnable<StructureCheckResult> cir) {
        if (!Thread.holdsLock(this)) {
            synchronized (this) {
                cir.setReturnValue(((StructureCheck) (Object) this)
                        .checkStart(chunkPos, structure, placement, skipKnownStructures));
            }
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
