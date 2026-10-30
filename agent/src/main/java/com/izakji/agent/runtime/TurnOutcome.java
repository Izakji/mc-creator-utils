package com.izakji.agent.runtime;

/**
 * Final outcome of one user turn.
 *
 * @author Izakji
 */
public enum TurnOutcome {
    COMPLETED,
    CANCELLED,
    MAX_ITERATIONS,
    ERROR
}
