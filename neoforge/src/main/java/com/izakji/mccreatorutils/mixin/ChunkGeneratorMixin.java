package com.izakji.mccreatorutils.mixin;

import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes structure reference claims atomic across the structure lane and normal generation.
 *
 * @author Izakji
 */
@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {

    @Inject(method = "tryAddReference", at = @At("HEAD"), cancellable = true)
    private static void mccreatorutils$claimReferenceAtomically(
            StructureManager structureManager,
            StructureStart structureStart,
            CallbackInfoReturnable<Boolean> cir) {
        synchronized (structureStart) {
            if (structureStart.canBeReferenced()) {
                structureManager.addReference(structureStart);
                cir.setReturnValue(true);
            } else {
                cir.setReturnValue(false);
            }
        }
    }
}
