package com.izakji.agent.runtime;

import com.google.gson.JsonObject;
import com.izakji.agent.api.ToolRisk;
import java.util.Objects;
import java.util.UUID;

/**
 * Request shown to the user before a mutating tool call.
 *
 * @param id approval id
 * @param toolName tool name
 * @param risk effective risk
 * @param arguments tool arguments
 * @param displayText short user-facing description
 * @author Izakji
 */
public record ApprovalRequest(
        String id, String toolName, ToolRisk risk, JsonObject arguments, String displayText) {
    public ApprovalRequest {
        id = Objects.requireNonNullElse(id, UUID.randomUUID().toString());
        toolName = Objects.requireNonNull(toolName, "toolName");
        risk = Objects.requireNonNull(risk, "risk");
        arguments = Objects.requireNonNull(arguments, "arguments").deepCopy();
        displayText = Objects.requireNonNullElse(displayText, toolName);
    }

    @Override
    public JsonObject arguments() {
        return arguments.deepCopy();
    }
}
