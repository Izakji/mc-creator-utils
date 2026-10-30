package com.izakji.agent.config;

import java.util.Objects;
import java.util.Set;

/**
 * Permission policy for tool calls.
 *
 * @param readOnlyAutoApprove whether read-only tools run without approval
 * @param allowedTools tool names always allowed
 * @param deniedTools tool names always denied
 * @param enabledDangerousTools dangerous tool names explicitly enabled
 * @author Izakji
 */
public record PermissionRules(
        boolean readOnlyAutoApprove,
        Set<String> allowedTools,
        Set<String> deniedTools,
        Set<String> enabledDangerousTools) {

    public PermissionRules {
        allowedTools = Set.copyOf(Objects.requireNonNullElse(allowedTools, Set.of()));
        deniedTools = Set.copyOf(Objects.requireNonNullElse(deniedTools, Set.of()));
        enabledDangerousTools = Set.copyOf(Objects.requireNonNullElse(enabledDangerousTools, Set.of()));
    }
}
