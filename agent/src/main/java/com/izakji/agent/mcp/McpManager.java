package com.izakji.agent.mcp;

import com.izakji.agent.runtime.ToolRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Lifecycle and tool registration for configured MCP servers.
 *
 * @author Izakji
 */
public final class McpManager implements AutoCloseable {
    private final Map<String, McpClient> clients;
    private final Map<String, McpServerConfig> configs;
    private final ToolRegistry tools = new ToolRegistry();
    private final Map<String, List<String>> registered = new LinkedHashMap<>();

    public McpManager(Map<String, McpClient> clients, Map<String, McpServerConfig> configs) {
        this.clients = Map.copyOf(Objects.requireNonNullElse(clients, Map.of()));
        this.configs = Map.copyOf(Objects.requireNonNullElse(configs, Map.of()));
    }

    public void start(String serverId) {
        McpClient client = clients.get(serverId);
        if (client == null) {
            throw new IllegalArgumentException("Unknown MCP server: " + serverId);
        }
        stop(serverId);
        client.initialize().join();
        List<String> names = new ArrayList<>();
        for (McpToolDescriptor descriptor : client.listTools().join()) {
            McpTool tool = new McpTool(client, serverId, descriptor);
            tools.register(tool);
            names.add(tool.name());
        }
        McpResourceTool resourceTool = new McpResourceTool(client, serverId);
        McpPromptTool promptTool = new McpPromptTool(client, serverId);
        tools.register(resourceTool);
        tools.register(promptTool);
        names.add(resourceTool.name());
        names.add(promptTool.name());
        registered.put(serverId, names);
    }

    public void stop(String serverId) {
        List<String> names = registered.remove(serverId);
        if (names != null) {
            names.forEach(tools::unregister);
        }
        McpClient client = clients.get(serverId);
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

    @Override
    public void close() {
        List.copyOf(registered.keySet()).forEach(this::stop);
    }
}
