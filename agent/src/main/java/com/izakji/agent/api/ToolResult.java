package com.izakji.agent.api;

import java.util.List;
import java.util.Objects;

/**
 * Result returned by a tool.
 *
 * @param callId id of the answered call
 * @param content textual result
 * @param error whether the tool failed
 * @param attachments optional images or other content
 * @author Izakji
 */
public record ToolResult(String callId, String content, boolean error, List<ContentPart> attachments) {
    public ToolResult {
        callId = Objects.requireNonNull(callId, "callId");
        content = content == null ? "" : content;
        attachments = List.copyOf(Objects.requireNonNull(attachments, "attachments"));
    }
}
