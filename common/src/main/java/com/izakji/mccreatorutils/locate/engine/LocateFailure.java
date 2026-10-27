package com.izakji.mccreatorutils.locate.engine;

/**
 * Outcomes that prevent a locate request from returning a position.
 *
 * @author Izakji
 */
public enum LocateFailure {
    QUEUE_FULL,
    TIMEOUT,
    CANCELLED,
    NOT_FOUND,
    ERROR
}
