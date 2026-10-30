package com.izakji.agent.api;

/**
 * Reason a provider stream ended.
 *
 * @author Izakji
 */
public enum FinishReason {
    STOP,
    TOOL_CALLS,
    LENGTH,
    ERROR
}
