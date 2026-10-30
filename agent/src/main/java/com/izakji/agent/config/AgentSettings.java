package com.izakji.agent.config;

import java.util.List;
import java.util.Objects;

/**
 * Agent settings persisted in the client config.
 *
 * @param defaultProfileId default provider profile id
 * @param profiles named provider profiles
 * @param permissions permission policy
 * @param maxToolRounds maximum provider/tool rounds per user turn
 * @param maxToolCalls maximum tool calls per user turn
 * @param approvalTimeoutSeconds approval wait before denying
 * @author Izakji
 */
public record AgentSettings(
        String defaultProfileId,
        List<ProviderProfile> profiles,
        PermissionRules permissions,
        int maxToolRounds,
        int maxToolCalls,
        int approvalTimeoutSeconds) {

    public AgentSettings {
        defaultProfileId = defaultProfileId == null ? "" : defaultProfileId.strip();
        profiles = List.copyOf(Objects.requireNonNullElse(profiles, List.of()));
        permissions = Objects.requireNonNullElseGet(
                permissions,
                () -> new PermissionRules(true, java.util.Set.of(), java.util.Set.of(), java.util.Set.of()));
        if (maxToolRounds < 1 || maxToolCalls < 1 || approvalTimeoutSeconds < 1) {
            throw new IllegalArgumentException("agent limits must be positive");
        }
        String selectedProfile = defaultProfileId;
        if (!profiles.isEmpty()
                && profiles.stream().noneMatch(profile -> profile.id().equals(selectedProfile))) {
            throw new IllegalArgumentException("defaultProfileId must match a profile");
        }
    }
}
