package com.izakji.agent.runtime;

import com.izakji.agent.api.ToolRisk;
import java.util.Objects;

/**
 * Permission decision, effective risk, and optional explanation.
 *
 * @param kind decision kind
 * @param reason human-readable reason
 * @param risk effective risk after classifier adjustments
 * @author Izakji
 */
public record PermissionDecision(PermissionKind kind, String reason, ToolRisk risk) {
    public PermissionDecision {
        kind = Objects.requireNonNull(kind, "kind");
        reason = Objects.requireNonNullElse(reason, "");
        risk = Objects.requireNonNull(risk, "risk");
    }

    public static PermissionDecision allow(String reason, ToolRisk risk) {
        return new PermissionDecision(PermissionKind.ALLOW, reason, risk);
    }

    public static PermissionDecision ask(String reason, ToolRisk risk) {
        return new PermissionDecision(PermissionKind.ASK, reason, risk);
    }

    public static PermissionDecision deny(String reason, ToolRisk risk) {
        return new PermissionDecision(PermissionKind.DENY, reason, risk);
    }
}
