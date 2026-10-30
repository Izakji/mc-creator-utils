package com.izakji.agent.plugin;

import com.izakji.agent.api.AgentTool;
import com.izakji.agent.api.LlmProvider;
import com.izakji.agent.mcp.McpServerConfig;
import com.izakji.agent.runtime.ContextProvider;
import com.izakji.agent.runtime.ToolRiskClassifier;
import com.izakji.agent.skill.Skill;

/**
 * Registration surface exposed to Java plugins.
 *
 * @author Izakji
 */
public interface AgentPluginContext {
    void registerTool(AgentTool tool);

    void registerContextProvider(ContextProvider provider);

    void registerProvider(LlmProvider provider);

    void registerSkill(Skill skill);

    void registerMcpServer(McpServerConfig config);

    void registerRiskClassifier(ToolRiskClassifier classifier);
}
