package com.izakji.agent.conversation;

import com.izakji.agent.api.AssistantMessage;
import com.izakji.agent.api.ChatMessage;
import com.izakji.agent.api.TextPart;
import com.izakji.agent.api.ToolCall;
import com.izakji.agent.api.ToolMessage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Repairs interrupted turns so every assistant tool call has a matching tool message.
 *
 * @author Izakji
 */
final class ConversationRepair {
    private ConversationRepair() {}

    static Conversation repair(Conversation conversation, Instant now) {
        List<ChatMessage> messages = conversation.messages();
        List<ChatMessage> repaired = new ArrayList<>();
        boolean changed = false;
        for (int index = 0; index < messages.size(); index++) {
            ChatMessage message = messages.get(index);
            repaired.add(message);
            if (!(message instanceof AssistantMessage assistant)
                    || assistant.toolCalls().isEmpty()) {
                continue;
            }
            Set<String> answered = new HashSet<>();
            int next = index + 1;
            while (next < messages.size() && messages.get(next) instanceof ToolMessage tool) {
                answered.add(tool.toolCallId());
                next++;
            }
            for (ToolCall call : assistant.toolCalls()) {
                if (!answered.contains(call.id())) {
                    repaired.add(new ToolMessage(
                            call.id(), call.name(), List.of(new TextPart("Tool call did not complete")), true));
                    changed = true;
                }
            }
        }
        if (!changed) {
            return conversation;
        }
        return new Conversation(
                conversation.id(),
                conversation.title(),
                conversation.created(),
                now,
                conversation.profileId(),
                repaired,
                conversation.conversationApprovals());
    }
}
