package com.izakji.agent.mcp;

import java.util.Set;

/**
 * MCP initialize response.
 *
 * @param name server name
 * @param version server version
 * @param protocolVersion negotiated protocol version
 * @param capabilities advertised capability names
 * @author Izakji
 */
public record McpServerInfo(String name, String version, String protocolVersion, Set<String> capabilities) {
    public McpServerInfo {
        capabilities = Set.copyOf(capabilities);
    }
}
