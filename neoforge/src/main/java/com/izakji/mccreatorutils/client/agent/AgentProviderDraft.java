package com.izakji.mccreatorutils.client.agent;

import com.izakji.agent.config.ProviderProfile;
import com.izakji.agent.config.ProviderType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Editable provider fields that never expose an existing stored key.
 *
 * @param displayName display name
 * @param type provider protocol
 * @param baseUrl API base URL
 * @param model model id
 * @param apiKey replacement key, blank to keep the stored key
 * @param temperature sampling temperature
 * @param maxOutputTokens maximum output tokens
 * @param contextWindow context window
 * @param tools whether tool calling is supported
 * @param thinkingBudgetTokens extended-thinking budget, or zero
 * @param headers custom request headers
 * @author Izakji
 */
public record AgentProviderDraft(
        String displayName,
        ProviderType type,
        String baseUrl,
        String model,
        String apiKey,
        double temperature,
        int maxOutputTokens,
        int contextWindow,
        boolean tools,
        int thinkingBudgetTokens,
        Map<String, String> headers) {

    public AgentProviderDraft(
            String displayName,
            ProviderType type,
            String baseUrl,
            String model,
            String apiKey,
            double temperature,
            int maxOutputTokens,
            int contextWindow,
            boolean tools,
            int thinkingBudgetTokens) {
        this(
                displayName,
                type,
                baseUrl,
                model,
                apiKey,
                temperature,
                maxOutputTokens,
                contextWindow,
                tools,
                thinkingBudgetTokens,
                Map.of());
    }

    public AgentProviderDraft {
        displayName = displayName == null ? "" : displayName.strip();
        type = type == null ? ProviderType.OPENAI_COMPATIBLE : type;
        baseUrl = baseUrl == null ? "" : baseUrl.strip();
        model = model == null ? "" : model.strip();
        apiKey = apiKey == null ? "" : apiKey;
        headers = Map.copyOf(headers == null ? Map.of() : headers);
    }

    /**
     * Copies a stored profile without exposing its key.
     *
     * @param profile stored profile
     * @return draft
     */
    public static AgentProviderDraft from(ProviderProfile profile) {
        return new AgentProviderDraft(
                profile.displayName(),
                profile.type(),
                profile.baseUrl(),
                profile.model(),
                "",
                profile.temperature(),
                profile.maxOutputTokens(),
                profile.contextWindow(),
                profile.tools(),
                profile.thinkingBudgetTokens(),
                profile.headers());
    }

    public AgentProviderDraft withApiKey(String value) {
        return new AgentProviderDraft(
                displayName,
                type,
                baseUrl,
                model,
                value,
                temperature,
                maxOutputTokens,
                contextWindow,
                tools,
                thinkingBudgetTokens,
                headers);
    }

    /**
     * Returns human-readable validation failures in display order.
     *
     * @return validation errors
     */
    public List<String> validationErrors() {
        List<String> errors = new ArrayList<>();
        if (displayName.isBlank()) {
            errors.add("Display name is required");
        }
        if (baseUrl.isBlank()) {
            errors.add("Base URL is required");
        } else if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            errors.add("Base URL must start with http:// or https://");
        }
        if (model.isBlank()) {
            errors.add("Model is required");
        }
        return List.copyOf(errors);
    }

    /**
     * Builds a profile, using the stored key when the draft key is blank.
     *
     * @param id profile id
     * @param existingApiKey stored key
     * @return profile
     */
    public ProviderProfile toProfile(String id, String existingApiKey) {
        String key = apiKey.isBlank() ? (existingApiKey == null ? "" : existingApiKey) : apiKey;
        return new ProviderProfile(
                id,
                displayName,
                type,
                baseUrl,
                model,
                key,
                headers,
                temperature,
                maxOutputTokens,
                contextWindow,
                true,
                tools,
                thinkingBudgetTokens);
    }
}
