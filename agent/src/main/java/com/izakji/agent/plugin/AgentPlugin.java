package com.izakji.agent.plugin;

/**
 * Compiled extension point for other mods.
 *
 * @author Izakji
 */
public interface AgentPlugin {
    String id();

    void register(AgentPluginContext context);
}
