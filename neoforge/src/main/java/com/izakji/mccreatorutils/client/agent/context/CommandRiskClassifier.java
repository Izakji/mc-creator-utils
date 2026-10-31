package com.izakji.mccreatorutils.client.agent.context;

import com.izakji.agent.api.AgentTool;
import com.izakji.agent.api.ToolInvocation;
import com.izakji.agent.api.ToolRisk;
import com.izakji.agent.runtime.ToolRiskClassifier;

/**
 * Auto-approves only the read-only command subset; everything else requires approval.
 *
 * @author Izakji
 */
public final class CommandRiskClassifier implements ToolRiskClassifier {
    @Override
    public ToolRisk classify(AgentTool tool, ToolInvocation invocation, ToolRisk declared) {
        if (!"run_command".equals(tool.name())) {
            return declared;
        }
        return CommandSafety.isReadOnly(invocation.arguments()) ? ToolRisk.READ_ONLY : ToolRisk.MUTATING;
    }
}
