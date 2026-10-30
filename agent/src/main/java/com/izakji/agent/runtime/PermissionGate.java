package com.izakji.agent.runtime;

import com.izakji.agent.api.AgentTool;
import com.izakji.agent.api.ToolInvocation;
import com.izakji.agent.api.ToolRisk;
import com.izakji.agent.config.PermissionRules;
import com.izakji.agent.conversation.Conversation;
import java.util.List;
import java.util.Objects;

/**
 * Applies risk tiers and user rules to tool calls.
 *
 * @author Izakji
 */
public final class PermissionGate {
    private final PermissionRules rules;
    private final List<ToolRiskClassifier> classifiers;

    public PermissionGate(PermissionRules rules, List<ToolRiskClassifier> classifiers) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.classifiers = List.copyOf(Objects.requireNonNullElse(classifiers, List.of()));
    }

    public PermissionDecision decide(
            AgentTool tool, ToolInvocation invocation, Conversation conversation) {
        ToolRisk risk = tool.risk();
        for (ToolRiskClassifier classifier : classifiers) {
            risk = higher(risk, classifier.classify(tool, invocation, risk));
        }
        String name = tool.name();
        if (rules.deniedTools().contains(name)) {
            return PermissionDecision.deny("Tool is denied by configuration");
        }
        if (risk == ToolRisk.DANGEROUS) {
            return rules.enabledDangerousTools().contains(name)
                    ? PermissionDecision.allow("Dangerous tool explicitly enabled")
                    : PermissionDecision.deny("Dangerous tool requires explicit enablement");
        }
        if (rules.allowedTools().contains(name)) {
            return PermissionDecision.allow("Tool is allowed by configuration");
        }
        if (conversation.conversationApprovals().contains(name) && tool.allowsConversationApproval()) {
            return PermissionDecision.allow("Tool is approved for this conversation");
        }
        if (risk == ToolRisk.READ_ONLY && rules.readOnlyAutoApprove()) {
            return PermissionDecision.allow("Read-only tool");
        }
        return PermissionDecision.ask("Tool requires user approval");
    }

    private static ToolRisk higher(ToolRisk left, ToolRisk right) {
        if (right == null) {
            return left;
        }
        return left.ordinal() >= right.ordinal() ? left : right;
    }
}
