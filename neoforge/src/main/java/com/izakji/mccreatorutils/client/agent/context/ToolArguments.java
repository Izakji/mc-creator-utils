package com.izakji.mccreatorutils.client.agent.context;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Tolerant accessors for model-provided tool arguments.
 *
 * @author Izakji
 */
final class ToolArguments {
    private ToolArguments() {}

    static String string(JsonObject arguments, String name, String fallback) {
        if (arguments == null) {
            return fallback;
        }
        JsonElement value = arguments.get(name);
        if (value == null || !value.isJsonPrimitive()) {
            return fallback;
        }
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        return primitive.isString() ? primitive.getAsString() : fallback;
    }

    static boolean bool(JsonObject arguments, String name, boolean fallback) {
        if (arguments == null) {
            return fallback;
        }
        JsonElement value = arguments.get(name);
        if (value == null || !value.isJsonPrimitive()) {
            return fallback;
        }
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        try {
            return primitive.isBoolean() ? primitive.getAsBoolean() : Boolean.parseBoolean(primitive.getAsString());
        } catch (UnsupportedOperationException invalid) {
            return fallback;
        }
    }

    static int integer(JsonObject arguments, String name, int fallback) {
        if (arguments == null) {
            return fallback;
        }
        JsonElement value = arguments.get(name);
        if (value == null || !value.isJsonPrimitive()) {
            return fallback;
        }
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        try {
            return primitive.isNumber() ? primitive.getAsInt() : Integer.parseInt(primitive.getAsString());
        } catch (NumberFormatException invalid) {
            return fallback;
        } catch (UnsupportedOperationException invalid) {
            return fallback;
        }
    }
}
