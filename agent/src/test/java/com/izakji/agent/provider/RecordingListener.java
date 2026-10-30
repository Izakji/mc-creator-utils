package com.izakji.agent.provider;

import com.izakji.agent.api.FinishReason;
import com.izakji.agent.api.StreamListener;
import com.izakji.agent.api.ToolCall;
import java.util.ArrayList;
import java.util.List;

class RecordingListener implements StreamListener {
    final StringBuilder text = new StringBuilder();
    final StringBuilder reasoning = new StringBuilder();
    final List<ToolCall> toolCalls = new ArrayList<>();
    String signature = "";
    FinishReason finish;

    @Override
    public void onTextDelta(String delta) {
        text.append(delta);
    }

    @Override
    public void onReasoningDelta(String delta) {
        reasoning.append(delta);
    }

    @Override
    public void onReasoningSignature(String value) {
        signature = value;
    }

    @Override
    public void onToolCall(ToolCall call) {
        toolCalls.add(call);
    }

    @Override
    public void onUsage(int promptTokens, int completionTokens) {}

    @Override
    public void onFinish(FinishReason reason) {
        finish = reason;
    }
}
