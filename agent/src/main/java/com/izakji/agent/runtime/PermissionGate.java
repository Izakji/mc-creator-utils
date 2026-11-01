package com.izakji.agent.runtime;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.izakji.agent.api.AgentTool;
import com.izakji.agent.api.ToolInvocation;
import com.izakji.agent.api.ToolRisk;
import com.izakji.agent.config.PermissionRules;
import com.izakji.agent.conversation.Conversation;
import java.util.ArrayList;
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

    public PermissionDecision decide(AgentTool tool, ToolInvocation invocation, Conversation conversation) {
        ToolRisk risk = tool.risk();
        for (ToolRiskClassifier classifier : classifiers) {
            risk = higher(risk, classifier.classify(tool, invocation, risk));
        }
        String name = tool.name();
        if (rules.deniedTools().contains(name)) {
            return PermissionDecision.deny("Tool is denied by configuration", risk);
        }
        if (risk == ToolRisk.DANGEROUS) {
            return rules.enabledDangerousTools().contains(name)
                    ? PermissionDecision.allow("Dangerous tool explicitly enabled", risk)
                    : PermissionDecision.deny("Dangerous tool requires explicit enablement", risk);
        }
        if (rules.allowedTools().contains(name)) {
            return PermissionDecision.allow("Tool is allowed by configuration", risk);
        }
        if (conversation.conversationApprovals().contains(approvalKey(name, invocation.arguments()))
                && tool.allowsConversationApproval()) {
            return PermissionDecision.allow("Tool is approved for this conversation", risk);
        }
        if (risk == ToolRisk.READ_ONLY && rules.readOnlyAutoApprove()) {
            return PermissionDecision.allow("Read-only tool", risk);
        }
        return PermissionDecision.ask("Tool requires user approval", risk);
    }

    /**
     * Builds the conversation-approval key for one exact tool call.
     *
     * @param toolName tool name
     * @param arguments call arguments
     * @return approval key
     */
    public static String approvalKey(String toolName, JsonObject arguments) {
        StringBuilder key = new StringBuilder(toolName == null ? "" : toolName);
        appendCanonical(arguments == null ? new JsonObject() : arguments, key);
        return key.toString();
    }

    private static void appendCanonical(JsonElement element, StringBuilder key) {
        if (element == null || element.isJsonNull()) {
            key.append("null");
        } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            key.append('{');
            List<String> names = new ArrayList<>(object.keySet());
            names.sort(String::compareTo);
            boolean first = true;
            for (String name : names) {
                if (!first) {
                    key.append(',');
                }
                first = false;
                key.append(new com.google.gson.JsonPrimitive(name)).append(':');
                appendCanonical(object.get(name), key);
            }
            key.append('}');
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            key.append('[');
            for (int index = 0; index < array.size(); index++) {
                if (index > 0) {
                    key.append(',');
                }
                appendCanonical(array.get(index), key);
            }
            key.append(']');
        } else {
            key.append(element);
        }
    }

    private static ToolRisk higher(ToolRisk left, ToolRisk right) {
        if (right == null) {
            return left;
        }
        return left.ordinal() >= right.ordinal() ? left : right;
    }
}
