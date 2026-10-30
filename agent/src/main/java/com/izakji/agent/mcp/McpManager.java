package com.izakji.agent.mcp;

import com.izakji.agent.runtime.ToolRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Lifecycle and tool registration for configured MCP servers.
 *
 * @author Izakji
 */
public final class McpManager implements AutoCloseable {
    private final Map<String, Supplier<McpClient>> factories;
    private final Map<String, McpServerConfig> configs;
    private final Map<String, McpClient> active = new LinkedHashMap<>();
    private final ToolRegistry tools = new ToolRegistry();
    private final Map<String, List<String>> registered = new LinkedHashMap<>();

    public McpManager(Map<String, Supplier<McpClient>> factories, Map<String, McpServerConfig> configs) {
        this.factories = Map.copyOf(Objects.requireNonNullElse(factories, Map.of()));
        this.configs = Map.copyOf(Objects.requireNonNullElse(configs, Map.of()));
    }

    public void start(String serverId) {
        Supplier<McpClient> factory = factories.get(serverId);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown MCP server: " + serverId);
        }
        if (active.containsKey(serverId)) {
            stop(serverId);
        }
        McpClient client = factory.get();
        active.put(serverId, client);
        McpServerInfo info = client.initialize().join();
        List<String> names = new ArrayList<>();
        for (McpToolDescriptor descriptor : client.listTools().join()) {
            McpTool tool = new McpTool(client, serverId, descriptor);
            tools.register(tool);
            names.add(tool.name());
        }
        if (info.capabilities().contains("resources")) {
            McpResourceTool resourceTool = new McpResourceTool(client, serverId);
            tools.register(resourceTool);
            names.add(resourceTool.name());
        }
        if (info.capabilities().contains("prompts")) {
            McpPromptTool promptTool = new McpPromptTool(client, serverId);
            tools.register(promptTool);
            names.add(promptTool.name());
        }
        registered.put(serverId, names);
    }

    public void stop(String serverId) {
        List<String> names = registered.remove(serverId);
        if (names != null) {
            names.forEach(tools::unregister);
        }
        McpClient client = active.remove(serverId);
        if (client != null) {
            client.close();
        }
    }

    public ToolRegistry tools() {
        return tools;
    }

    public Map<String, McpServerConfig> configs() {
        return configs;
    }

    /**
     * Returns MCP header and environment values so the runtime can redact them from prompts,
     * tool output, and persisted conversations.
     *
     * @return configured secret values
     */
    public List<String> secrets() {
        List<String> values = new ArrayList<>();
        for (McpServerConfig config : configs.values()) {
            values.addAll(config.headers().values());
            values.addAll(config.environment().values());
        }
        return List.copyOf(values);
    }

    @Override
    public void close() {
        List.copyOf(active.keySet()).forEach(this::stop);
    }
}
