package com.izakji.agent.runtime;

import com.google.gson.JsonObject;
import com.izakji.agent.api.TextPart;
import com.izakji.agent.api.ToolCall;
import com.izakji.agent.api.UserMessage;
import java.util.List;

final class TestMessages {
    private TestMessages() {}

    static UserMessage user(String text) {
        return new UserMessage(List.of(new TextPart(text)));
    }

    static ToolCall call(String id) {
        return new ToolCall(id, "mutate", new JsonObject());
    }
}
