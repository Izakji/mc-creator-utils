package com.izakji.mccreatorutils.client.agent.context;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Client-thread registry search with a short cache.
 *
 * @author Izakji
 */
final class RegistrySearchReader {
    private static final long CACHE_MILLIS = 5 * 60_000;
    private static final Map<RegistryKind, CachedIds> CACHE = new EnumMap<>(RegistryKind.class);
    private static WeakReference<Level> cachedLevel = new WeakReference<>(null);

    private RegistrySearchReader() {}

    static synchronized String search(RegistryKind kind, String query, String namespace, int limit) {
        List<ResourceLocation> ids = ids(kind);
        List<String> matches = new ArrayList<>();
        for (ResourceLocation id : ids) {
            if (RegistrySearchFilter.matches(id.toString(), query, namespace)) {
                matches.add(id.toString());
            }
        }
        if (matches.isEmpty()) {
            return "No matching registry entries.";
        }
        int shown = Math.min(Math.max(1, limit), matches.size());
        StringBuilder report = new StringBuilder();
        report.append("Registry ")
                .append(kind.id)
                .append(" (")
                .append(shown)
                .append(" of ")
                .append(matches.size())
                .append("):\n");
        for (int index = 0; index < shown; index++) {
            report.append("- ").append(matches.get(index)).append('\n');
        }
        return report.toString();
    }

    private static List<ResourceLocation> ids(RegistryKind kind) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level != cachedLevel.get()) {
            CACHE.clear();
            cachedLevel = new WeakReference<>(level);
        }
        CachedIds current = CACHE.get(kind);
        long now = System.currentTimeMillis();
        if (current != null && now - current.builtAt < CACHE_MILLIS) {
            return current.ids;
        }
        List<ResourceLocation> ids = buildIds(kind, minecraft, level);
        CACHE.put(kind, new CachedIds(ids, now));
        return ids;
    }

    private static List<ResourceLocation> buildIds(RegistryKind kind, Minecraft minecraft, Level level) {
        List<ResourceLocation> ids = new ArrayList<>();
        switch (kind) {
            case ITEM ->
                BuiltInRegistries.ITEM.keySet().forEach(id -> {
                    if (!id.equals(ResourceLocation.parse("minecraft:air"))) {
                        ids.add(id);
                    }
                });
            case BLOCK ->
                BuiltInRegistries.BLOCK.keySet().forEach(id -> {
                    if (!id.equals(ResourceLocation.parse("minecraft:air"))) {
                        ids.add(id);
                    }
                });
            case ENTITY -> ids.addAll(BuiltInRegistries.ENTITY_TYPE.keySet());
            case EFFECT -> ids.addAll(BuiltInRegistries.MOB_EFFECT.keySet());
            case BIOME -> ids.addAll(registryIds(level, Registries.BIOME));
            case STRUCTURE -> ids.addAll(registryIds(level, Registries.STRUCTURE));
            case ENCHANTMENT -> ids.addAll(registryIds(level, Registries.ENCHANTMENT));
            case DIMENSION -> {
                if (minecraft.getConnection() != null) {
                    minecraft.getConnection().levels().forEach(key -> ids.add(key.location()));
                }
                if (level != null && !ids.contains(level.dimension().location())) {
                    ids.add(level.dimension().location());
                }
            }
        }
        return List.copyOf(ids);
    }

    private static List<ResourceLocation> registryIds(Level level, ResourceKey<? extends Registry<?>> key) {
        if (level == null) {
            return List.of();
        }
        var registry = level.registryAccess().registry(key);
        return registry.map(Registry::keySet).map(List::copyOf).orElse(List.of());
    }

    private record CachedIds(List<ResourceLocation> ids, long builtAt) {}
}
