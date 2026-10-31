package com.izakji.mccreatorutils.client.agent.context;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;

/**
 * Lazily built, short-lived client recipe index.
 *
 * @author Izakji
 */
final class RecipeIndex {
    private static final long CACHE_MILLIS = 5 * 60_000;
    private static final int MAX_INDEX = 50_000;
    private static final int MAX_INPUTS = 12;
    private static List<RecipeEntry> cache = List.of();
    private static long cachedAt;
    private static WeakReference<Level> cachedLevel = new WeakReference<>(null);

    private RecipeIndex() {}

    static synchronized String lookup(String query, int limit) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) {
            throw new IllegalStateException("No world is loaded.");
        }
        long now = System.currentTimeMillis();
        if (level != cachedLevel.get()) {
            cache = List.of();
            cachedLevel = new WeakReference<>(level);
            cachedAt = 0;
        }
        if (cache.isEmpty() || now - cachedAt >= CACHE_MILLIS) {
            cache = build(level);
            cachedAt = now;
        }
        return RecipeReport.format(cache, query, limit);
    }

    private static List<RecipeEntry> build(Level level) {
        RecipeManager manager = level.getRecipeManager();
        List<RecipeEntry> entries = new ArrayList<>();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            if (entries.size() >= MAX_INDEX) {
                break;
            }
            RecipeEntry entry = entry(level, holder);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return List.copyOf(entries);
    }

    private static RecipeEntry entry(Level level, RecipeHolder<?> holder) {
        Recipe<?> recipe = holder.value();
        ItemStack result;
        try {
            result = recipe.getResultItem(level.registryAccess());
        } catch (RuntimeException ignored) {
            return null;
        }
        if (result.isEmpty()) {
            return null;
        }
        ResourceLocation outputId = BuiltInRegistries.ITEM.getKey(result.getItem());
        List<String> inputs = new ArrayList<>();
        try {
            for (Ingredient ingredient : recipe.getIngredients()) {
                ItemStack[] stacks = ingredient.getItems();
                if (stacks.length > 0 && !stacks[0].isEmpty()) {
                    inputs.add(
                            BuiltInRegistries.ITEM.getKey(stacks[0].getItem()).toString());
                }
                if (inputs.size() >= MAX_INPUTS) {
                    break;
                }
            }
        } catch (RuntimeException ignored) {
            // Keep the recipe with the inputs collected so far.
        }
        ResourceLocation typeId = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
        return new RecipeEntry(
                holder.id().toString(),
                typeId == null ? "unknown" : typeId.toString(),
                outputId.toString(),
                result.getCount(),
                inputs);
    }
}
