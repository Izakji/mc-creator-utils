package com.izakji.agent.runtime;

/**
 * Permission decision and optional explanation.
 *
 * @param kind decision kind
 * @param reason human-readable reason
 * @author Izakji
 */
public record PermissionDecision(PermissionKind kind, String reason) {
    public static PermissionDecision allow(String reason) {
        return new PermissionDecision(PermissionKind.ALLOW, reason);
    }

    public static PermissionDecision ask(String reason) {
        return new PermissionDecision(PermissionKind.ASK, reason);
    }

    public static PermissionDecision deny(String reason) {
        return new PermissionDecision(PermissionKind.DENY, reason);
    }
}
