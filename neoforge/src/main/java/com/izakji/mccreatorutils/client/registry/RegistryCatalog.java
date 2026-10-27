package com.izakji.mccreatorutils.client.registry;

import com.izakji.mccreatorutils.registry.RegistryGroup;
import com.izakji.mccreatorutils.registry.RegistryGrouping;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;

/**
 * Loads and labels the registries used by the browser tools.
 *
 * @author Izakji
 */
public final class RegistryCatalog {
    private static final ResourceLocation AIR = ResourceLocation.parse("minecraft:air");

    private RegistryCatalog() {}

    public static List<RegistryGroup> groups(RegistryKind kind) {
        List<RegistryGroup> groups = new ArrayList<>(RegistryGrouping.group(ids(kind)));
        groups.sort(
                Comparator.comparing((RegistryGroup group) -> !group.namespace().equals("minecraft"))
                        .thenComparing(group -> modName(group.namespace()).getString(), String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(groups);
    }

    public static Component label(RegistryKind kind, ResourceLocation id) {
        return switch (kind) {
            case ITEM -> {
                Item item = BuiltInRegistries.ITEM.get(id);
                yield item == null ? Component.literal(prettyPath(id.getPath())) : item.getDescription();
            }
            case MOB -> {
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
                yield type == null ? Component.literal(prettyPath(id.getPath())) : type.getDescription();
            }
            case BIOME ->
                Component.translatableWithFallback(
                        "biome." + id.getNamespace() + "." + id.getPath(), prettyPath(id.getPath()));
            case STRUCTURE -> Component.literal(prettyPath(id.getPath()));
            case DIMENSION ->
                Component.translatableWithFallback(
                        "dimension." + id.getNamespace() + "." + id.getPath(), prettyPath(id.getPath()));
        };
    }

    public static Component modName(String namespace) {
        if ("minecraft".equals(namespace)) {
            return Component.translatable("tool.mccreatorutils.registry.minecraft");
        }
        return ModList.get()
                .getModContainerById(namespace)
                .map(container -> Component.literal(container.getModInfo().getDisplayName()))
                .orElseGet(() -> Component.literal(namespace));
    }

    private static Collection<ResourceLocation> ids(RegistryKind kind) {
        return switch (kind) {
            case ITEM -> {
                Set<ResourceLocation> ids = new HashSet<>(BuiltInRegistries.ITEM.keySet());
                ids.remove(AIR);
                yield ids;
            }
            case MOB -> {
                List<ResourceLocation> ids = new ArrayList<>();
                for (ResourceLocation id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
                    EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
                    if (type != null && type != EntityType.PLAYER && type.getCategory() != MobCategory.MISC) {
                        ids.add(id);
                    }
                }
                yield ids;
            }
            case BIOME -> dynamicIds(Registries.BIOME);
            case STRUCTURE -> dynamicIds(Registries.STRUCTURE);
            case DIMENSION -> dimensionIds();
        };
    }

    private static Collection<ResourceLocation> dimensionIds() {
        Minecraft minecraft = Minecraft.getInstance();
        var connection = minecraft.getConnection();
        if (connection != null) {
            Set<ResourceLocation> ids = new HashSet<>();
            for (var key : connection.levels()) {
                ids.add(key.location());
            }
            if (!ids.isEmpty()) {
                return ids;
            }
        }

        var server = minecraft.getSingleplayerServer();
        if (server != null) {
            Set<ResourceLocation> ids = new HashSet<>();
            for (var key : server.levelKeys()) {
                ids.add(key.location());
            }
            if (!ids.isEmpty()) {
                return ids;
            }
        }

        var level = minecraft.level;
        return level == null ? List.of() : List.of(level.dimension().location());
    }

    private static <T> Collection<ResourceLocation> dynamicIds(
            net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<T>> registryKey) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            var registry = server.registryAccess().registry(registryKey);
            if (registry.isPresent()) {
                return registry.get().keySet();
            }
        }
        var level = Minecraft.getInstance().level;
        if (level != null) {
            var registry = level.registryAccess().registry(registryKey);
            if (registry.isPresent()) {
                return registry.get().keySet();
            }
        }
        return List.of();
    }

    private static String prettyPath(String path) {
        StringBuilder result = new StringBuilder();
        for (String word : path.split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(word.substring(0, 1).toUpperCase(Locale.ROOT));
            if (word.length() > 1) {
                result.append(word.substring(1));
            }
        }
        return result.isEmpty() ? path : result.toString();
    }
}
