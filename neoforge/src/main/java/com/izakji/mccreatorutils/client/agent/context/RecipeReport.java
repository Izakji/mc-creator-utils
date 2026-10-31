package com.izakji.mccreatorutils.client.agent.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Formats recipe matches.
 *
 * @author Izakji
 */
public final class RecipeReport {
    private RecipeReport() {}

    public static String format(List<RecipeEntry> recipes, String query, int limit) {
        String needle = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        List<RecipeEntry> matches = new ArrayList<>();
        for (RecipeEntry recipe : recipes) {
            if (needle.isEmpty()
                    || recipe.id().toLowerCase(Locale.ROOT).contains(needle)
                    || recipe.output().toLowerCase(Locale.ROOT).contains(needle)
                    || recipe.inputs().stream()
                            .anyMatch(input -> input.toLowerCase(Locale.ROOT).contains(needle))) {
                matches.add(recipe);
            }
        }
        if (matches.isEmpty()) {
            return "No matching recipes.";
        }
        int shown = Math.min(Math.max(1, limit), matches.size());
        StringBuilder report = new StringBuilder();
        report.append("Recipes (")
                .append(shown)
                .append(" of ")
                .append(matches.size())
                .append("):\n");
        for (int index = 0; index < shown; index++) {
            RecipeEntry recipe = matches.get(index);
            report.append("- ")
                    .append(recipe.id())
                    .append(" [")
                    .append(recipe.type())
                    .append("] -> ")
                    .append(recipe.output())
                    .append(" x")
                    .append(recipe.count())
                    .append(" inputs: ")
                    .append(String.join(", ", recipe.inputs()))
                    .append('\n');
        }
        return report.toString();
    }
}
