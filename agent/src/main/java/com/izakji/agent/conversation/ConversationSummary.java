package com.izakji.agent.conversation;

import java.time.Instant;

/**
 * Lightweight conversation list entry.
 *
 * @param id conversation id
 * @param title display title
 * @param updated last update time
 * @author Izakji
 */
public record ConversationSummary(String id, String title, Instant updated) {}
