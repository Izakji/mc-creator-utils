package com.izakji.agent.runtime;

import com.izakji.agent.api.AgentTool;
import com.izakji.agent.api.ToolInvocation;
import com.izakji.agent.api.ToolRisk;

/**
 * Adjusts a tool's declared risk from concrete arguments.
 *
 * @author Izakji
 */
@FunctionalInterface
public interface ToolRiskClassifier {
    ToolRisk classify(AgentTool tool, ToolInvocation invocation, ToolRisk declared);
}
