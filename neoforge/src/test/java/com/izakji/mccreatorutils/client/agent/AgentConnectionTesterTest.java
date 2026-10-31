package com.izakji.mccreatorutils.client.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.izakji.agent.api.CancellationToken;
import com.izakji.agent.api.ChatRequest;
import com.izakji.agent.api.FinishReason;
import com.izakji.agent.api.LlmProvider;
import com.izakji.agent.api.StreamListener;
import com.izakji.agent.config.ProviderProfile;
import com.izakji.agent.config.ProviderType;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class AgentConnectionTesterTest {

    @Test
    void returnsTheProviderText() {
        LlmProvider provider = new LlmProvider() {
            @Override
            public ProviderType type() {
                return ProviderType.OPENAI_COMPATIBLE;
            }

            @Override
            public boolean supportsTools() {
                return true;
            }

            @Override
            public CompletableFuture<Void> stream(
                    ChatRequest request, StreamListener listener, CancellationToken token) {
                listener.onTextDelta("OK");
                listener.onFinish(FinishReason.STOP);
                return CompletableFuture.completedFuture(null);
            }
        };
        ProviderProfile profile = new ProviderProfile(
                "openai",
                "OpenAI",
                ProviderType.OPENAI_COMPATIBLE,
                "https://example.invalid/v1",
                "gpt-4o-mini",
                "sk-test",
                Map.of(),
                0.7,
                2048,
                128000,
                true,
                true);
        assertEquals("OK", new AgentConnectionTester().test(provider, profile).join());
    }
}
