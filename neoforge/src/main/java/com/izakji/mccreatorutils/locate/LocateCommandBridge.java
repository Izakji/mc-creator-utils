package com.izakji.mccreatorutils.locate;

import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Thin command bridge between LocateCommand and the async coordinator.
 *
 * @author Izakji
 */
public final class LocateCommandBridge {
    private LocateCommandBridge() {}

    public static OptionalInt submitBiome(CommandSourceStack source, ResourceOrTagArgument.Result<Biome> target) {
        if (!(source.getEntity() instanceof ServerPlayer)
                || !LocateConfig.settings().enabled()) {
            return OptionalInt.empty();
        }
        LocateCoordinator coordinator = LocateLifecycle.coordinator();
        if (coordinator == null || LocateLifecycle.service() == null) {
            return OptionalInt.empty();
        }
        return coordinator.submitBiome(source, target);
    }

    public static OptionalInt submitStructure(
            CommandSourceStack source, ResourceOrTagKeyArgument.Result<Structure> target) {
        if (!(source.getEntity() instanceof ServerPlayer)
                || !LocateConfig.settings().enabled()) {
            return OptionalInt.empty();
        }
        LocateCoordinator coordinator = LocateLifecycle.coordinator();
        if (coordinator == null || LocateLifecycle.service() == null) {
            return OptionalInt.empty();
        }

        Registry<Structure> registry = source.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
        Optional<? extends HolderSet<Structure>> holders = target.unwrap()
                .map(
                        key -> registry.getHolder(key).map(holder -> (HolderSet<Structure>) HolderSet.direct(holder)),
                        registry::getTag);
        if (holders.isEmpty()) {
            return OptionalInt.empty();
        }
        return coordinator.submitStructure(source, holders.get(), target);
    }
}
