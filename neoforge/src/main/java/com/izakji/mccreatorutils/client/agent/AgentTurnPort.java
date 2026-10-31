package com.izakji.mccreatorutils.client.agent;

import com.izakji.agent.config.AgentSettings;
import com.izakji.agent.conversation.ConversationStore;
import com.izakji.agent.runtime.ApprovalChoice;
import com.izakji.agent.runtime.TurnHandle;
import com.izakji.agent.runtime.TurnObserver;

/**
 * Narrow runtime surface used by the chat session controller.
 *
 * @author Izakji
 */
public interface AgentTurnPort {
    AgentSettings settings();

    ConversationStore conversations();

    TurnHandle startTurn(String conversationId, String userText, TurnObserver observer);

    void approve(String conversationId, String approvalId, ApprovalChoice choice);
}
