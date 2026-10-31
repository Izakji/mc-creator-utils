package com.izakji.mccreatorutils.client.agent;

import com.izakji.agent.config.AgentSettings;
import com.izakji.agent.config.PermissionRules;
import com.izakji.agent.config.ProviderPresets;
import java.util.List;
import java.util.Set;

/**
 * Default client settings used when no agent configuration exists.
 *
 * @author Izakji
 */
public final class AgentDefaults {
    private AgentDefaults() {}

    /**
     * Returns the initial settings with one OpenAI profile and a blank key.
     *
     * @return default settings
     */
    public static AgentSettings settings() {
        return new AgentSettings(
                "openai",
                List.of(ProviderPresets.openAi("")),
                new PermissionRules(true, Set.of(), Set.of(), Set.of()),
                8,
                32,
                60);
    }
}
