package com.izakji.agent.skill;

import com.google.gson.JsonObject;
import com.izakji.agent.api.AgentTool;
import com.izakji.agent.api.ToolContext;
import com.izakji.agent.api.ToolInvocation;
import com.izakji.agent.api.ToolResult;
import com.izakji.agent.api.ToolRisk;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Read-only tool that returns a skill body by id.
 *
 * @author Izakji
 */
public final class LoadSkillTool implements AgentTool {
    private final SkillRegistry skills;

    public LoadSkillTool(SkillRegistry skills) {
        this.skills = skills;
    }

    @Override
    public String name() {
        return "load_skill";
    }

    @Override
    public String description() {
        return "Load a skill's instructions by id";
    }

    @Override
    public JsonObject inputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject properties = new JsonObject();
        JsonObject id = new JsonObject();
        id.addProperty("type", "string");
        properties.add("id", id);
        schema.add("properties", properties);
        com.google.gson.JsonArray required = new com.google.gson.JsonArray();
        required.add("id");
        schema.add("required", required);
        return schema;
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.READ_ONLY;
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolInvocation invocation, ToolContext context) {
        String id = invocation.arguments().has("id")
                ? invocation.arguments().get("id").getAsString()
                : "";
        return CompletableFuture.completedFuture(skills.find(id)
                .map(skill -> new ToolResult(invocation.callId(), skill.body(), false, List.of()))
                .orElseGet(() -> new ToolResult(invocation.callId(), "Unknown skill: " + id, true, List.of())));
    }
}
