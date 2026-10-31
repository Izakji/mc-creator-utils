package com.izakji.mccreatorutils.client.agent;

import com.google.gson.JsonObject;
import com.izakji.agent.api.AssistantMessage;
import com.izakji.agent.api.ChatMessage;
import com.izakji.agent.api.ContentPart;
import com.izakji.agent.api.TextPart;
import com.izakji.agent.api.ToolMessage;
import com.izakji.agent.api.UserMessage;
import com.izakji.agent.conversation.Conversation;
import com.izakji.agent.runtime.ApprovalRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mutable transcript shown in the agent chat window.
 *
 * @author Izakji
 */
public final class AgentChatModel {
    public enum Kind {
        USER,
        ASSISTANT,
        REASONING,
        TOOL,
        APPROVAL,
        ERROR,
        NOTICE
    }

    public record Entry(Kind kind, String title, String text, boolean error, String approvalId) {}

    private final List<Entry> entries = new ArrayList<>();
    private int assistantIndex = -1;
    private int reasoningIndex = -1;
    private ApprovalRequest pendingApproval;
    private long revision;

    public long revision() {
        return revision;
    }

    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    public Optional<ApprovalRequest> pendingApproval() {
        return Optional.ofNullable(pendingApproval);
    }

    public void clear() {
        entries.clear();
        assistantIndex = -1;
        reasoningIndex = -1;
        pendingApproval = null;
        revision++;
    }

    public void load(Conversation conversation) {
        clear();
        for (ChatMessage message : conversation.messages()) {
            switch (message) {
                case UserMessage user -> addUser(text(user.parts()));
                case AssistantMessage assistant -> {
                    for (ContentPart part : assistant.parts()) {
                        if (part instanceof TextPart text) {
                            appendAssistant(text.text());
                        }
                    }
                    if (!assistant.toolCalls().isEmpty()) {
                        assistantIndex = -1;
                        reasoningIndex = -1;
                    }
                }
                case ToolMessage tool -> {
                    // Tool activity stays internal; only assistant answers are shown.
                    assistantIndex = -1;
                    reasoningIndex = -1;
                }
                default -> {
                    // System messages are prompt material, not transcript entries.
                }
            }
        }
    }

    public void addUser(String text) {
        assistantIndex = -1;
        reasoningIndex = -1;
        add(Kind.USER, "You", text, false, "");
    }

    public void appendAssistant(String delta) {
        if (delta == null || delta.isEmpty()) {
            return;
        }
        if (assistantIndex < 0 || assistantIndex >= entries.size()) {
            assistantIndex = entries.size();
            add(Kind.ASSISTANT, "Agent", delta, false, "");
            assistantIndex = entries.size() - 1;
            return;
        }
        Entry current = entries.get(assistantIndex);
        entries.set(
                assistantIndex,
                new Entry(
                        current.kind(),
                        current.title(),
                        current.text() + delta,
                        current.error(),
                        current.approvalId()));
        revision++;
    }

    public void appendReasoning(String delta) {
        if (delta == null || delta.isEmpty()) {
            return;
        }
        if (reasoningIndex < 0 || reasoningIndex >= entries.size()) {
            reasoningIndex = entries.size();
            add(Kind.REASONING, "Reasoning", delta, false, "");
            reasoningIndex = entries.size() - 1;
            return;
        }
        Entry current = entries.get(reasoningIndex);
        entries.set(
                reasoningIndex,
                new Entry(
                        current.kind(),
                        current.title(),
                        current.text() + delta,
                        current.error(),
                        current.approvalId()));
        revision++;
    }

    public void addToolStarted(String name, JsonObject arguments) {
        assistantIndex = -1;
        reasoningIndex = -1;
    }

    public void addToolFinished(String name, boolean error, String content) {
        assistantIndex = -1;
        reasoningIndex = -1;
    }

    public void addApproval(ApprovalRequest request) {
        pendingApproval = request;
        add(
                Kind.APPROVAL,
                request.toolName(),
                request.displayText() + "\nRisk: " + request.risk() + "\n" + request.arguments(),
                request.risk() == com.izakji.agent.api.ToolRisk.DANGEROUS,
                request.id());
    }

    public void clearApproval() {
        pendingApproval = null;
        entries.removeIf(entry -> entry.kind() == Kind.APPROVAL);
        assistantIndex = -1;
        reasoningIndex = -1;
        revision++;
    }

    public void addError(String message) {
        assistantIndex = -1;
        reasoningIndex = -1;
        add(Kind.ERROR, "Error", message, true, "");
    }

    public void addNotice(String message) {
        add(Kind.NOTICE, "", message, false, "");
    }

    private void add(Kind kind, String title, String text, boolean error, String approvalId) {
        entries.add(new Entry(kind, title, text == null ? "" : text, error, approvalId));
        revision++;
    }

    private static String text(List<ContentPart> parts) {
        StringBuilder text = new StringBuilder();
        for (ContentPart part : parts) {
            if (part instanceof TextPart value) {
                if (!text.isEmpty()) {
                    text.append('\n');
                }
                text.append(value.text());
            }
        }
        return text.toString();
    }
}
