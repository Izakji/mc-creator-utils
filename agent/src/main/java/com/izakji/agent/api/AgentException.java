package com.izakji.agent.api;

/**
 * Recoverable agent configuration, storage, or runtime failure.
 *
 * @author Izakji
 */
public class AgentException extends Exception {
    public AgentException(String message) {
        super(message);
    }

    public AgentException(String message, Throwable cause) {
        super(message, cause);
    }
}
