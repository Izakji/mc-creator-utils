package com.izakji.agent.runtime;

/**
 * Result of one completed turn.
 *
 * @param outcome final outcome
 * @param conversationId conversation id
 * @param error error text, or an empty string
 * @author Izakji
 */
public record TurnResult(TurnOutcome outcome, String conversationId, String error) {}
