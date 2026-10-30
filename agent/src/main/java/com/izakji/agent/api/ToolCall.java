package com.izakji.agent.api;

import com.google.gson.JsonObject;
import java.util.Objects;

/**
 * Tool call requested by the model.
 *
 * @param id provider call id
 * @param name tool name
 * @param arguments JSON arguments
 * @author Izakji
 */
public record ToolCall(String id, String name, JsonObject arguments) {
    public ToolCall {
        id = Objects.requireNonNull(id, "id");
        name = Objects.requireNonNull(name, "name");
        arguments = Objects.requireNonNull(arguments, "arguments").deepCopy();
    }

    @Override
    public JsonObject arguments() {
        return arguments.deepCopy();
    }
}
