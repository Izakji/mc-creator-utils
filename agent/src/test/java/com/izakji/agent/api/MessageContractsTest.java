package com.izakji.agent.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.util.List;
import org.junit.jupiter.api.Test;

class MessageContractsTest {

    @Test
    void imagePartDefensivelyCopiesBytes() {
        byte[] source = {1, 2, 3};
        ImagePart part = new ImagePart("image/png", source);
        source[0] = 9;
        assertEquals(1, part.data()[0]);
        byte[] returned = part.data();
        returned[1] = 9;
        assertEquals(2, part.data()[1]);
    }

    @Test
    void toolCallDeepCopiesArguments() {
        JsonObject arguments = new JsonObject();
        arguments.addProperty("x", 1);
        ToolCall call = new ToolCall("id", "tool", arguments);
        arguments.addProperty("x", 2);
        assertEquals(1, call.arguments().get("x").getAsInt());
    }

    @Test
    void messageListsAreImmutable() {
        UserMessage message = new UserMessage(List.of(new TextPart("hi")));
        assertThrows(UnsupportedOperationException.class, () -> message.parts().add(new TextPart("x")));
    }

    @Test
    void toolResultCarriesErrorAndAttachments() {
        ToolResult result = new ToolResult("call-1", "failed", true, List.of(new TextPart("detail")));
        assertTrue(result.error());
        assertEquals("failed", result.content());
        assertEquals(1, result.attachments().size());
    }
}
