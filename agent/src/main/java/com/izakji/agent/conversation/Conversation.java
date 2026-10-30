package com.izakji.agent.conversation;

import com.izakji.agent.api.ChatMessage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * One named agent conversation.
 *
 * @param id conversation id
 * @param title display title
 * @param created creation time
 * @param updated last update time
 * @param profileId provider profile used by the conversation
 * @param messages immutable message history
 * @param conversationApprovals tool names approved for this conversation
 * @author Izakji
 */
public record Conversation(
        String id,
        String title,
        Instant created,
        Instant updated,
        String profileId,
        List<ChatMessage> messages,
        Set<String> conversationApprovals) {

    public Conversation {
        id = Objects.requireNonNull(id, "id");
        title = normalizeTitle(title);
        created = Objects.requireNonNull(created, "created");
        updated = Objects.requireNonNull(updated, "updated");
        profileId = profileId == null ? "" : profileId;
        messages = List.copyOf(Objects.requireNonNull(messages, "messages"));
        conversationApprovals = Set.copyOf(Objects.requireNonNull(conversationApprovals, "conversationApprovals"));
    }

    /**
     * Returns a copy with one message appended.
     *
     * @param message message to append
     * @param now update timestamp
     * @return updated conversation
     */
    public Conversation withMessage(ChatMessage message, Instant now) {
        List<ChatMessage> next = new ArrayList<>(messages);
        next.add(Objects.requireNonNull(message, "message"));
        return new Conversation(id, title, created, now, profileId, next, conversationApprovals);
    }

    /**
     * Returns a copy with a new title.
     *
     * @param title new title
     * @param now update timestamp
     * @return updated conversation
     */
    public Conversation withTitle(String title, Instant now) {
        return new Conversation(id, title, created, now, profileId, messages, conversationApprovals);
    }

    /**
     * Returns a copy with a tool approved for this conversation.
     *
     * @param toolName tool name
     * @param now update timestamp
     * @return updated conversation
     */
    public Conversation withConversationApproval(String toolName, Instant now) {
        Set<String> next = new java.util.LinkedHashSet<>(conversationApprovals);
        next.add(Objects.requireNonNull(toolName, "toolName"));
        return new Conversation(id, title, created, now, profileId, messages, next);
    }

    static String normalizeTitle(String title) {
        String value = title == null ? "" : title.strip();
        if (value.isEmpty()) {
            value = "New Chat";
        }
        return value.length() <= 80 ? value : value.substring(0, 80).strip();
    }
}
