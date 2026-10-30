package com.izakji.agent.api;

import com.izakji.agent.config.ProviderProfile;
import java.util.List;
import java.util.Objects;

/**
 * One provider request.
 *
 * @param profile provider profile
 * @param messages conversation messages
 * @param tools tools exposed to the model
 * @param maxOutputTokens output token cap
 * @author Izakji
 */
public record ChatRequest(
        ProviderProfile profile, List<ChatMessage> messages, List<ToolSchema> tools, int maxOutputTokens) {
    public ChatRequest {
        profile = Objects.requireNonNull(profile, "profile");
        messages = List.copyOf(Objects.requireNonNull(messages, "messages"));
        tools = List.copyOf(Objects.requireNonNull(tools, "tools"));
        maxOutputTokens = Math.max(1, maxOutputTokens);
    }
}
