package com.izakji.agent.mcp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

final class FakeTransport implements McpTransport {
    private Consumer<JsonObject> listener = ignored -> {};
    private final List<JsonObject> sent = new ArrayList<>();

    @Override
    public CompletableFuture<Void> start(Consumer<JsonObject> listener) {
        this.listener = listener;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void send(JsonObject message) {
        sent.add(message.deepCopy());
    }

    @Override
    public void close() {}

    void receive(String json) {
        listener.accept(JsonParser.parseString(json).getAsJsonObject());
    }

    List<JsonObject> sent() {
        return List.copyOf(sent);
    }
}
