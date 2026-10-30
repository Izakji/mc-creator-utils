package com.izakji.agent.config;

import java.util.Map;

/**
 * Built-in provider profile presets.
 *
 * @author Izakji
 */
public final class ProviderPresets {
    private static final int DEFAULT_MAX_OUTPUT = 2048;
    private static final int DEFAULT_CONTEXT_WINDOW = 128_000;

    private ProviderPresets() {}

    public static ProviderProfile openAi(String apiKey) {
        return profile("openai", "OpenAI", ProviderType.OPENAI_COMPATIBLE,
                "https://api.openai.com/v1", "gpt-4o-mini", apiKey);
    }

    public static ProviderProfile openRouter(String apiKey) {
        return profile("openrouter", "OpenRouter", ProviderType.OPENAI_COMPATIBLE,
                "https://openrouter.ai/api/v1", "openai/gpt-4o-mini", apiKey);
    }

    public static ProviderProfile deepSeek(String apiKey) {
        return profile("deepseek", "DeepSeek", ProviderType.OPENAI_COMPATIBLE,
                "https://api.deepseek.com", "deepseek-chat", apiKey);
    }

    public static ProviderProfile groq(String apiKey) {
        return profile("groq", "Groq", ProviderType.OPENAI_COMPATIBLE,
                "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile", apiKey);
    }

    public static ProviderProfile ollama(String apiKey) {
        return profile("ollama", "Ollama", ProviderType.OPENAI_COMPATIBLE,
                "http://localhost:11434/v1", "llama3.2", apiKey);
    }

    public static ProviderProfile lmStudio(String apiKey) {
        return profile("lmstudio", "LM Studio", ProviderType.OPENAI_COMPATIBLE,
                "http://localhost:1234/v1", "", apiKey);
    }

    public static ProviderProfile anthropic(String apiKey) {
        return profile("anthropic", "Anthropic", ProviderType.ANTHROPIC,
                "https://api.anthropic.com", "claude-sonnet-4-5", apiKey);
    }

    public static ProviderProfile custom(ProviderType type) {
        return profile("custom", "Custom", type, "https://api.openai.com/v1", "", "");
    }

    private static ProviderProfile profile(
            String id, String displayName, ProviderType type, String baseUrl, String model, String apiKey) {
        return new ProviderProfile(
                id,
                displayName,
                type,
                baseUrl,
                model,
                apiKey,
                Map.of(),
                0.7,
                DEFAULT_MAX_OUTPUT,
                DEFAULT_CONTEXT_WINDOW,
                true,
                true);
    }
}
