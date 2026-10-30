package com.izakji.agent.plugin;

import com.izakji.agent.api.AgentException;
import com.izakji.agent.api.AgentTool;
import com.izakji.agent.api.LlmProvider;
import com.izakji.agent.mcp.McpServerConfig;
import com.izakji.agent.runtime.ContextProvider;
import com.izakji.agent.runtime.ToolRegistry;
import com.izakji.agent.runtime.ToolRiskClassifier;
import com.izakji.agent.skill.Skill;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Collects contributions from compiled Java plugins.
 *
 * @author Izakji
 */
public final class AgentPluginRegistry implements AgentPluginContext {
    private final Set<String> pluginIds = new LinkedHashSet<>();
    private final ToolRegistry tools = new ToolRegistry();
    private final List<ContextProvider> contextProviders = new ArrayList<>();
    private final List<LlmProvider> providers = new ArrayList<>();
    private final List<Skill> skills = new ArrayList<>();
    private final Map<String, McpServerConfig> mcpServers = new LinkedHashMap<>();
    private final List<ToolRiskClassifier> riskClassifiers = new ArrayList<>();

    public void register(AgentPlugin plugin) throws AgentException {
        if (!pluginIds.add(plugin.id())) {
            throw new AgentException("Plugin already registered: " + plugin.id());
        }
        try {
            plugin.register(this);
        } catch (RuntimeException failure) {
            throw new AgentException("Plugin failed to register: " + plugin.id(), failure);
        }
    }

    public ToolRegistry tools() {
        return tools;
    }

    public List<ContextProvider> contextProviders() {
        return List.copyOf(contextProviders);
    }

    public List<LlmProvider> providers() {
        return List.copyOf(providers);
    }

    public List<Skill> skills() {
        return List.copyOf(skills);
    }

    public Map<String, McpServerConfig> mcpServers() {
        return Map.copyOf(mcpServers);
    }

    public List<ToolRiskClassifier> riskClassifiers() {
        return List.copyOf(riskClassifiers);
    }

    @Override
    public void registerTool(AgentTool tool) {
        tools.register(tool);
    }

    @Override
    public void registerContextProvider(ContextProvider provider) {
        contextProviders.add(provider);
    }

    @Override
    public void registerProvider(LlmProvider provider) {
        providers.add(provider);
    }

    @Override
    public void registerSkill(Skill skill) {
        skills.add(skill);
    }

    @Override
    public void registerMcpServer(McpServerConfig config) {
        if (mcpServers.putIfAbsent(config.id(), config) != null) {
            throw new IllegalArgumentException("MCP server already registered: " + config.id());
        }
    }

    @Override
    public void registerRiskClassifier(ToolRiskClassifier classifier) {
        riskClassifiers.add(classifier);
    }
}
