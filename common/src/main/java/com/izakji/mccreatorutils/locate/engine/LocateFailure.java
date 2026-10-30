package com.izakji.mccreatorutils.locate.engine;

/**
 * Outcomes that prevent a locate request from returning a position.
 *
 * @author Izakji
 */
public enum LocateFailure {
    /** Admission rejected the request because the queue is full. */
    QUEUE_FULL,
    /** The request deadline passed. */
    TIMEOUT,
    /** The request was cancelled by its callers or by shutdown. */
    CANCELLED,
    /** The search completed without finding a target. */
    NOT_FOUND,
    /** The search threw an unexpected exception. */
    ERROR
}
