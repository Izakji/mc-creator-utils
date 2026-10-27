package com.izakji.mccreatorutils.mixin;

import com.izakji.mccreatorutils.locate.LocateCommandBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.server.commands.LocateCommand;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Routes player locate commands through the async engine.
 *
 * @author Izakji
 */
@Mixin(LocateCommand.class)
public class LocateCommandMixin {

    @Inject(method = "locateStructure", at = @At("HEAD"), cancellable = true)
    private static void mccreatorutils$locateStructure(
            CommandSourceStack source,
            ResourceOrTagKeyArgument.Result<Structure> target,
            CallbackInfoReturnable<Integer> cir) {
        LocateCommandBridge.submitStructure(source, target).ifPresent(result -> cir.setReturnValue(result));
    }

    @Inject(method = "locateBiome", at = @At("HEAD"), cancellable = true)
    private static void mccreatorutils$locateBiome(
            CommandSourceStack source,
            ResourceOrTagArgument.Result<Biome> target,
            CallbackInfoReturnable<Integer> cir) {
        LocateCommandBridge.submitBiome(source, target).ifPresent(result -> cir.setReturnValue(result));
    }
}
