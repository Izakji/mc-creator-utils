package com.izakji.agent.runtime;

import com.izakji.agent.api.AgentTool;
import com.izakji.agent.api.ToolSchema;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Registered tools available to the agent.
 *
 * @author Izakji
 */
public final class ToolRegistry {
    private final Map<String, AgentTool> tools = new LinkedHashMap<>();

    public void register(AgentTool tool) {
        Objects.requireNonNull(tool, "tool");
        String name = tool.name().strip();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("tool name must not be blank");
        }
        if (tools.putIfAbsent(name, tool) != null) {
            throw new IllegalArgumentException("Tool already registered: " + name);
        }
    }

    public Optional<AgentTool> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public void unregister(String name) {
        tools.remove(name);
    }

    public List<ToolSchema> schemas() {
        return tools.values().stream()
                .map(tool -> new ToolSchema(tool.name(), tool.description(), tool.inputSchema()))
                .sorted((left, right) -> left.name().compareTo(right.name()))
                .toList();
    }
}
