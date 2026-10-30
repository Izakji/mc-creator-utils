package com.izakji.agent.api;

/**
 * Streaming provider callbacks.
 *
 * @author Izakji
 */
public interface StreamListener {
    void onTextDelta(String delta);

    void onReasoningDelta(String delta);

    void onToolCall(ToolCall call);

    void onUsage(int promptTokens, int completionTokens);

    void onFinish(FinishReason reason);
}
