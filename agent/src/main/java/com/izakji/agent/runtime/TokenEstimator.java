package com.izakji.agent.runtime;

import com.izakji.agent.api.ChatMessage;
import com.izakji.agent.api.ContentPart;
import com.izakji.agent.api.ImagePart;
import com.izakji.agent.api.ReasoningPart;
import com.izakji.agent.api.TextPart;
import com.izakji.agent.api.ToolCall;
import com.izakji.agent.api.ToolMessage;
import com.izakji.agent.api.UserMessage;
import com.izakji.agent.api.AssistantMessage;
import java.util.List;

/**
 * Coarse token estimator used for context trimming.
 *
 * @author Izakji
 */
public final class TokenEstimator {
    private static final int MESSAGE_OVERHEAD = 4;

    public int estimate(ChatMessage message) {
        int tokens = MESSAGE_OVERHEAD;
        switch (message) {
            case com.izakji.agent.api.SystemMessage system -> tokens += textTokens(system.text());
            case UserMessage user -> tokens += partsTokens(user.parts());
            case AssistantMessage assistant -> {
                tokens += partsTokens(assistant.parts());
                for (ToolCall call : assistant.toolCalls()) {
                    tokens += 4 + textTokens(call.name()) + textTokens(call.arguments().toString());
                }
            }
            case ToolMessage tool -> tokens += partsTokens(tool.parts());
        }
        return tokens;
    }

    public int estimate(List<ChatMessage> messages) {
        return messages.stream().mapToInt(this::estimate).sum();
    }

    private int partsTokens(List<ContentPart> parts) {
        int tokens = 0;
        for (ContentPart part : parts) {
            if (part instanceof TextPart text) {
                tokens += textTokens(text.text());
            } else if (part instanceof ReasoningPart reasoning) {
                tokens += textTokens(reasoning.text());
            } else if (part instanceof ImagePart image) {
                tokens += Math.max(1, image.data().length / 1024);
            }
        }
        return tokens;
    }

    private static int textTokens(String text) {
        return text == null || text.isEmpty() ? 0 : (text.length() + 3) / 4;
    }
}
