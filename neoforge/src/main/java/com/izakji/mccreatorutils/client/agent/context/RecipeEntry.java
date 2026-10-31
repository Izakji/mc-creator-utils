package com.izakji.mccreatorutils.client.agent.context;

import java.util.List;

/**
 * One indexed recipe.
 *
 * @param id recipe id
 * @param type recipe type
 * @param output output item id
 * @param count output count
 * @param inputs input item ids
 * @author Izakji
 */
public record RecipeEntry(String id, String type, String output, int count, List<String> inputs) {
    public RecipeEntry {
        inputs = List.copyOf(inputs);
    }
}
