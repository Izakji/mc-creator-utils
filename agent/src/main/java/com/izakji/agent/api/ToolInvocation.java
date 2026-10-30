package com.izakji.agent.api;

import com.google.gson.JsonObject;
import java.util.Objects;

/**
 * Tool invocation passed to a tool implementation.
 *
 * @param callId provider call id
 * @param name tool name
 * @param arguments JSON arguments
 * @author Izakji
 */
public record ToolInvocation(String callId, String name, JsonObject arguments) {
    public ToolInvocation {
        callId = Objects.requireNonNull(callId, "callId");
        name = Objects.requireNonNull(name, "name");
        arguments = Objects.requireNonNull(arguments, "arguments").deepCopy();
    }

    @Override
    public JsonObject arguments() {
        return arguments.deepCopy();
    }
}
