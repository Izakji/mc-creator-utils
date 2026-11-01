package com.izakji.mccreatorutils.client.agent.context;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

class ToolArgumentsTest {

    @Test
    void toleratesMalformedValues() {
        JsonObject arguments = new JsonObject();
        arguments.add("text", com.google.gson.JsonNull.INSTANCE);
        arguments.add("object", new JsonObject());
        arguments.add("number", new com.google.gson.JsonPrimitive(7));
        arguments.add("string", new com.google.gson.JsonPrimitive("12"));
        assertEquals("", ToolArguments.string(arguments, "text", ""));
        assertEquals("", ToolArguments.string(arguments, "object", ""));
        assertEquals("", ToolArguments.string(arguments, "number", ""));
        assertEquals(5, ToolArguments.integer(arguments, "text", 5));
        assertEquals(5, ToolArguments.integer(arguments, "object", 5));
        assertEquals(7, ToolArguments.integer(arguments, "number", 5));
        assertEquals(12, ToolArguments.integer(arguments, "string", 5));
    }
}
