package com.izakji.agent.api;

/**
 * Provider-neutral chat message.
 *
 * @author Izakji
 */
public sealed interface ChatMessage permits SystemMessage, UserMessage, AssistantMessage, ToolMessage {
    ChatRole role();
}
