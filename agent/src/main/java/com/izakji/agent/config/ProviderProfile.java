package com.izakji.agent.config;

import java.util.Map;
import java.util.Objects;

/**
 * One named provider configuration.
 *
 * <p>The API key is deliberately omitted from {@link #toString()} so profile dumps and
 * error messages cannot leak it.</p>
 *
 * @param id stable profile id
 * @param displayName user-facing profile name
 * @param type provider protocol
 * @param baseUrl API base URL
 * @param model model id, or empty until the user selects one
 * @param apiKey API key, or empty for local providers
 * @param headers extra request headers
 * @param temperature sampling temperature
 * @param maxOutputTokens maximum output tokens
 * @param contextWindow context window in tokens
 * @param stream whether to request streaming responses
 * @param tools whether the model supports tool calling
 * @author Izakji
 */
public record ProviderProfile(
        String id,
        String displayName,
        ProviderType type,
        String baseUrl,
        String model,
        String apiKey,
        Map<String, String> headers,
        double temperature,
        int maxOutputTokens,
        int contextWindow,
        boolean stream,
        boolean tools) {

    public ProviderProfile {
        id = required(id, "id");
        displayName = required(displayName, "displayName");
        type = Objects.requireNonNull(type, "type");
        baseUrl = required(baseUrl, "baseUrl");
        model = model == null ? "" : model.strip();
        apiKey = apiKey == null ? "" : apiKey;
        headers = Map.copyOf(Objects.requireNonNullElse(headers, Map.of()));
        temperature = Math.max(0.0, Math.min(2.0, temperature));
        maxOutputTokens = Math.max(1, maxOutputTokens);
        contextWindow = Math.max(1024, contextWindow);
    }

    @Override
    public String toString() {
        return "ProviderProfile[id=" + id + ", displayName=" + displayName + ", type=" + type
                + ", baseUrl=" + baseUrl + ", model=" + model + ", temperature=" + temperature
                + ", maxOutputTokens=" + maxOutputTokens + ", contextWindow=" + contextWindow
                + ", stream=" + stream + ", tools=" + tools + ", apiKey=<redacted>]";
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.strip();
    }
}
