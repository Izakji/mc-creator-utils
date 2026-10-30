package com.izakji.agent.mcp;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * One configured MCP server.
 *
 * @param id stable server id
 * @param name display name
 * @param transport transport type
 * @param command stdio command, or null
 * @param args stdio arguments
 * @param workingDirectory stdio working directory, or null
 * @param environment stdio environment additions
 * @param url HTTP endpoint, or null
 * @param headers HTTP headers
 * @param enabled whether the server may be started
 * @param autoStart whether startup should start it automatically
 * @author Izakji
 */
public record McpServerConfig(
        String id,
        String name,
        McpTransportType transport,
        String command,
        List<String> args,
        String workingDirectory,
        Map<String, String> environment,
        String url,
        Map<String, String> headers,
        boolean enabled,
        boolean autoStart) {

    public McpServerConfig {
        id = Objects.requireNonNull(id, "id");
        name = name == null || name.isBlank() ? id : name.strip();
        transport = Objects.requireNonNull(transport, "transport");
        args = List.copyOf(Objects.requireNonNullElse(args, List.of()));
        environment = Map.copyOf(Objects.requireNonNullElse(environment, Map.of()));
        headers = Map.copyOf(Objects.requireNonNullElse(headers, Map.of()));
    }

    @Override
    public String toString() {
        return "McpServerConfig[id=" + id + ", name=" + name + ", transport=" + transport + ", enabled=" + enabled
                + ", autoStart=" + autoStart + "]";
    }
}
