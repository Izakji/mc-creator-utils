package com.izakji.agent.provider;

import com.izakji.agent.api.LlmProvider;
import com.izakji.agent.config.ProviderType;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of provider protocol adapters.
 *
 * @author Izakji
 */
public final class ProviderRegistry {
    private final Map<ProviderType, LlmProvider> providers = new EnumMap<>(ProviderType.class);

    public void register(LlmProvider provider) {
        if (providers.putIfAbsent(provider.type(), provider) != null) {
            throw new IllegalArgumentException("Provider already registered: " + provider.type());
        }
    }

    public Optional<LlmProvider> forType(ProviderType type) {
        return Optional.ofNullable(providers.get(type));
    }
}
