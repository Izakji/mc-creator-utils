package com.izakji.agent.api;

/**
 * Permission risk assigned to a tool call.
 *
 * @author Izakji
 */
public enum ToolRisk {
    READ_ONLY,
    MUTATING,
    DANGEROUS
}
