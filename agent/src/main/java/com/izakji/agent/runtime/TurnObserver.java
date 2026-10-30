package com.izakji.agent.runtime;

import com.izakji.agent.api.AgentException;
import com.izakji.agent.api.ToolCall;
import com.izakji.agent.api.ToolResult;

/**
 * UI callbacks for one agent turn.
 *
 * @author Izakji
 */
public interface TurnObserver {
    void onTextDelta(String delta);

    void onReasoningDelta(String delta);

    void onToolStarted(ToolCall call);

    void onToolFinished(ToolResult result);

    void onApprovalNeeded(ApprovalRequest request);

    void onError(AgentException error);

    void onFinished(TurnResult result);
}
