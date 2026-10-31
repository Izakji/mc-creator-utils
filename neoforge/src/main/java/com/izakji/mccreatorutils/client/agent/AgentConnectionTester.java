package com.izakji.mccreatorutils.client.agent;

import com.izakji.agent.api.ChatRequest;
import com.izakji.agent.api.FinishReason;
import com.izakji.agent.api.LlmProvider;
import com.izakji.agent.api.StreamListener;
import com.izakji.agent.api.TextPart;
import com.izakji.agent.api.ToolCall;
import com.izakji.agent.api.UserMessage;
import com.izakji.agent.config.ProviderProfile;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Sends one tiny chat request to verify a provider profile.
 *
 * @author Izakji
 */
public final class AgentConnectionTester {

    /**
     * Tests one provider call.
     *
     * @param provider provider adapter
     * @param profile profile to test
     * @return provider text, or {@code Connected} when the provider returned no text
     */
    public CompletableFuture<String> test(LlmProvider provider, ProviderProfile profile) {
        StringBuilder text = new StringBuilder();
        StreamListener listener = new StreamListener() {
            @Override
            public void onTextDelta(String delta) {
                text.append(delta);
            }

            @Override
            public void onReasoningDelta(String delta) {}

            @Override
            public void onToolCall(ToolCall call) {}

            @Override
            public void onUsage(int promptTokens, int completionTokens) {}

            @Override
            public void onFinish(FinishReason reason) {}
        };
        ChatRequest request = new ChatRequest(
                profile, List.of(new UserMessage(List.of(new TextPart("Reply with OK.")))), List.of(), 32);
        return provider.stream(request, listener, () -> false)
                .thenApply(ignored -> text.toString().isBlank()
                        ? "Connected"
                        : text.toString().strip());
    }
}
