package com.izakji.agent.provider;

import java.util.ArrayList;
import java.util.List;

/**
 * Incremental server-sent-events parser.
 *
 * @author Izakji
 */
public final class SseParser {
    private final StringBuilder pending = new StringBuilder();
    private final StringBuilder data = new StringBuilder();
    private String eventName = "";
    private boolean hasData;
    private boolean done;

    /**
     * Consumes a chunk and returns all complete events it finished.
     *
     * @param chunk raw text chunk
     * @return complete events
     */
    public List<SseEvent> accept(String chunk) {
        if (done || chunk == null || chunk.isEmpty()) {
            return List.of();
        }
        pending.append(chunk);
        List<SseEvent> events = new ArrayList<>();
        int newline;
        while ((newline = pending.indexOf("\n")) >= 0) {
            String line = pending.substring(0, newline);
            pending.delete(0, newline + 1);
            if (line.endsWith("\r")) {
                line = line.substring(0, line.length() - 1);
            }
            processLine(line, events);
            if (done) {
                pending.setLength(0);
                break;
            }
        }
        return events;
    }

    private void processLine(String line, List<SseEvent> events) {
        if (line.isEmpty()) {
            if (hasData) {
                String value = data.toString();
                if ("[DONE]".equals(value)) {
                    done = true;
                } else {
                    events.add(new SseEvent(eventName, value));
                }
            }
            data.setLength(0);
            eventName = "";
            hasData = false;
            return;
        }
        if (line.startsWith(":")) {
            return;
        }
        int colon = line.indexOf(':');
        String field = colon < 0 ? line : line.substring(0, colon);
        String value = colon < 0 ? "" : line.substring(colon + 1);
        if (value.startsWith(" ")) {
            value = value.substring(1);
        }
        if ("event".equals(field)) {
            eventName = value;
        } else if ("data".equals(field)) {
            if (hasData) {
                data.append('\n');
            }
            data.append(value);
            hasData = true;
        }
    }
}
