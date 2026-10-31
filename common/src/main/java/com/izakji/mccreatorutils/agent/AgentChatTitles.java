package com.izakji.mccreatorutils.agent;

/**
 * Derives a bounded conversation title from the first user message.
 *
 * @author Izakji
 */
public final class AgentChatTitles {
    private static final int MAX_LENGTH = 80;

    private AgentChatTitles() {}

    /**
     * Returns the first non-empty line, collapsed to single spaces.
     *
     * @param message first user message
     * @return bounded title
     */
    public static String fromFirstMessage(String message) {
        if (message == null) {
            return "New Chat";
        }
        String firstLine = message.strip().split("\\R", 2)[0].strip().replaceAll("\\s+", " ");
        if (firstLine.isEmpty()) {
            return "New Chat";
        }
        return firstLine.length() <= MAX_LENGTH
                ? firstLine
                : firstLine.substring(0, MAX_LENGTH).strip();
    }
}
