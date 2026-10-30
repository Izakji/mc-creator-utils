package com.izakji.agent.runtime;

import com.izakji.agent.api.AssistantMessage;
import com.izakji.agent.api.ChatMessage;
import com.izakji.agent.api.SystemMessage;
import com.izakji.agent.api.ToolMessage;
import com.izakji.agent.api.UserMessage;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Drops oldest complete turns while preserving system messages and the newest user request.
 *
 * @author Izakji
 */
public final class ConversationTruncator {
    public static final String TRUNCATION_NOTE = "Earlier conversation history was truncated.";
    private final TokenEstimator estimator;

    public ConversationTruncator(TokenEstimator estimator) {
        this.estimator = estimator;
    }

    public List<ChatMessage> truncate(List<ChatMessage> messages, int maxTokens) {
        return truncateDetailed(messages, maxTokens).messages();
    }

    public TruncationResult truncateDetailed(List<ChatMessage> messages, int maxTokens) {
        if (messages == null || messages.isEmpty() || estimator.estimate(messages) <= maxTokens) {
            return new TruncationResult(messages == null ? List.of() : List.copyOf(messages), false);
        }
        List<Unit> units = group(messages);
        int latestUser = -1;
        for (int index = 0; index < units.size(); index++) {
            if (units.get(index).containsUser()) {
                latestUser = index;
            }
        }
        Set<Integer> kept = new LinkedHashSet<>();
        int total = 0;
        if (latestUser < 0) {
            latestUser = units.size() - 1;
        }
        for (int index = latestUser; index < units.size(); index++) {
            kept.add(index);
            total += units.get(index).tokens(estimator);
        }
        for (int index = latestUser - 1; index >= 0; index--) {
            Unit unit = units.get(index);
            int cost = unit.tokens(estimator);
            if (total + cost <= maxTokens) {
                kept.add(index);
                total += cost;
            }
        }
        List<ChatMessage> result = new ArrayList<>();
        result.add(new SystemMessage(TRUNCATION_NOTE));
        for (int index = 0; index < units.size(); index++) {
            if (kept.contains(index)) {
                result.addAll(units.get(index).messages());
            }
        }
        return new TruncationResult(List.copyOf(result), true);
    }

    private static List<Unit> group(List<ChatMessage> messages) {
        List<Unit> units = new ArrayList<>();
        for (int index = 0; index < messages.size(); index++) {
            ChatMessage message = messages.get(index);
            if (message instanceof AssistantMessage assistant
                    && !assistant.toolCalls().isEmpty()) {
                List<ChatMessage> group = new ArrayList<>();
                group.add(message);
                int next = index + 1;
                while (next < messages.size() && messages.get(next) instanceof ToolMessage) {
                    group.add(messages.get(next));
                    next++;
                }
                units.add(new Unit(List.copyOf(group)));
                index = next - 1;
            } else if (message instanceof ToolMessage) {
                // An unmatched tool result is never safe to keep.
                continue;
            } else {
                units.add(new Unit(List.of(message)));
            }
        }
        return units;
    }

    public record TruncationResult(List<ChatMessage> messages, boolean truncated) {
        public TruncationResult {
            messages = List.copyOf(messages);
        }
    }

    private record Unit(List<ChatMessage> messages) {
        boolean containsUser() {
            return messages.stream().anyMatch(UserMessage.class::isInstance);
        }

        int tokens(TokenEstimator estimator) {
            return estimator.estimate(messages);
        }
    }
}
