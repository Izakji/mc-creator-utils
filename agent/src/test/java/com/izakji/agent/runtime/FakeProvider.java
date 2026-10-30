package com.izakji.agent.runtime;

import com.izakji.agent.api.CancellationToken;
import com.izakji.agent.api.ChatRequest;
import com.izakji.agent.api.FinishReason;
import com.izakji.agent.api.LlmProvider;
import com.izakji.agent.api.StreamListener;
import com.izakji.agent.api.ToolCall;
import com.izakji.agent.config.ProviderType;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

final class FakeProvider implements LlmProvider {
    sealed interface Step permits Turn, Failure {}

    record Turn(List<String> textDeltas, List<String> reasoningDeltas, List<ToolCall> toolCalls) implements Step {}

    record Failure(String message) implements Step {}

    private final List<Step> steps;
    private final AtomicInteger requests = new AtomicInteger();

    FakeProvider(List<Step> steps) {
        this.steps = List.copyOf(steps);
    }

    int requests() {
        return requests.get();
    }

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
            ChatRequest request, StreamListener listener, CancellationToken cancellationToken) {
        int index = requests.getAndIncrement();
        if (index >= steps.size()) {
            return CompletableFuture.failedFuture(new IllegalStateException("No fake step"));
        }
        Step step = steps.get(index);
        if (step instanceof Failure failure) {
            return CompletableFuture.failedFuture(new IllegalStateException(failure.message()));
        }
        Turn turn = (Turn) step;
        turn.textDeltas().forEach(listener::onTextDelta);
        turn.reasoningDeltas().forEach(listener::onReasoningDelta);
        turn.toolCalls().forEach(listener::onToolCall);
        listener.onFinish(turn.toolCalls().isEmpty() ? FinishReason.STOP : FinishReason.TOOL_CALLS);
        return CompletableFuture.completedFuture(null);
    }
}
