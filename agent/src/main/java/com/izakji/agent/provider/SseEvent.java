package com.izakji.agent.provider;

/**
 * One complete server-sent event.
 *
 * @param event event name, or an empty string when omitted
 * @param data event data
 * @author Izakji
 */
public record SseEvent(String event, String data) {
    public SseEvent {
        event = event == null ? "" : event;
        data = data == null ? "" : data;
    }
}
