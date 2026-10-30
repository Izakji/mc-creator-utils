package com.izakji.agent.api;

/**
 * One provider-neutral message content part.
 *
 * @author Izakji
 */
public sealed interface ContentPart permits TextPart, ReasoningPart, ImagePart {
    String type();
}
