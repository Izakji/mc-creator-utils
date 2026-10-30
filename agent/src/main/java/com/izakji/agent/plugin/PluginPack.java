package com.izakji.agent.plugin;

import com.izakji.agent.mcp.McpServerConfig;
import com.izakji.agent.skill.Skill;
import java.util.List;

/**
 * One declarative plugin pack.
 *
 * @param id pack id
 * @param name display name
 * @param version pack version
 * @param skills skills supplied by the pack
 * @param mcpServers MCP servers supplied by the pack
 * @author Izakji
 */
public record PluginPack(
        String id, String name, String version, List<Skill> skills, List<McpServerConfig> mcpServers) {}
