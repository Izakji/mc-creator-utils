package com.izakji.agent.conversation;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.izakji.agent.api.AgentException;
import com.izakji.agent.api.AssistantMessage;
import com.izakji.agent.api.ChatMessage;
import com.izakji.agent.api.ChatRole;
import com.izakji.agent.api.ContentPart;
import com.izakji.agent.api.ImagePart;
import com.izakji.agent.api.ReasoningPart;
import com.izakji.agent.api.SystemMessage;
import com.izakji.agent.api.TextPart;
import com.izakji.agent.api.ToolCall;
import com.izakji.agent.api.ToolMessage;
import com.izakji.agent.api.UserMessage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Explicit JSON codec for conversations and provider-neutral messages.
 *
 * @author Izakji
 */
final class ConversationJson {
    private final Gson gson;

    ConversationJson(Gson gson) {
        this.gson = gson;
    }

    String write(Conversation conversation) {
        JsonObject root = new JsonObject();
        root.addProperty("id", conversation.id());
        root.addProperty("title", conversation.title());
        root.addProperty("created", conversation.created().toString());
        root.addProperty("updated", conversation.updated().toString());
        root.addProperty("profileId", conversation.profileId());
        JsonArray approvals = new JsonArray();
        conversation.conversationApprovals().forEach(approvals::add);
        root.add("conversationApprovals", approvals);

        JsonArray messages = new JsonArray();
        conversation.messages().forEach(message -> messages.add(writeMessage(message)));
        root.add("messages", messages);
        return gson.toJson(root);
    }

    Conversation read(String json) throws AgentException {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            List<ChatMessage> messages = new ArrayList<>();
            for (JsonElement element : requiredArray(root, "messages")) {
                messages.add(readMessage(element.getAsJsonObject()));
            }
            Set<String> approvals = new LinkedHashSet<>();
            for (JsonElement element : requiredArray(root, "conversationApprovals")) {
                approvals.add(element.getAsString());
            }
            return new Conversation(
                    requiredString(root, "id"),
                    requiredString(root, "title"),
                    Instant.parse(requiredString(root, "created")),
                    Instant.parse(requiredString(root, "updated")),
                    requiredString(root, "profileId"),
                    messages,
                    approvals);
        } catch (RuntimeException failure) {
            throw new AgentException("Could not read conversation", failure);
        }
    }

    private JsonObject writeMessage(ChatMessage message) {
        JsonObject result = new JsonObject();
        result.addProperty("role", message.role().name());
        JsonArray parts = new JsonArray();
        switch (message) {
            case SystemMessage system -> parts.add(writePart(new TextPart(system.text())));
            case UserMessage user -> user.parts().forEach(part -> parts.add(writePart(part)));
            case AssistantMessage assistant -> {
                assistant.parts().forEach(part -> parts.add(writePart(part)));
                JsonArray calls = new JsonArray();
                assistant.toolCalls().forEach(call -> calls.add(writeCall(call)));
                result.add("toolCalls", calls);
            }
            case ToolMessage tool -> {
                result.addProperty("toolCallId", tool.toolCallId());
                result.addProperty("name", tool.name());
                result.addProperty("error", tool.error());
                tool.parts().forEach(part -> parts.add(writePart(part)));
            }
        }
        result.add("parts", parts);
        return result;
    }

    private JsonObject writePart(ContentPart part) {
        JsonObject result = new JsonObject();
        result.addProperty("type", part.type());
        switch (part) {
            case TextPart text -> result.addProperty("text", text.text());
            case ReasoningPart reasoning -> {
                result.addProperty("text", reasoning.text());
                result.addProperty("signature", reasoning.signature());
            }
            case ImagePart image -> {
                result.addProperty("mediaType", image.mediaType());
                result.addProperty("data", Base64.getEncoder().encodeToString(image.data()));
            }
        }
        return result;
    }

    private JsonObject writeCall(ToolCall call) {
        JsonObject result = new JsonObject();
        result.addProperty("id", call.id());
        result.addProperty("name", call.name());
        result.add("arguments", call.arguments());
        return result;
    }

    private ChatMessage readMessage(JsonObject object) {
        ChatRole role = ChatRole.valueOf(requiredString(object, "role"));
        List<ContentPart> parts = readParts(object);
        return switch (role) {
            case SYSTEM -> new SystemMessage(
                    parts.stream().filter(TextPart.class::isInstance)
                            .map(TextPart.class::cast)
                            .findFirst()
                            .map(TextPart::text)
                            .orElse(""));
            case USER -> new UserMessage(parts);
            case ASSISTANT -> {
                List<ToolCall> calls = new ArrayList<>();
                if (object.has("toolCalls")) {
                    for (JsonElement element : object.getAsJsonArray("toolCalls")) {
                        JsonObject call = element.getAsJsonObject();
                        calls.add(new ToolCall(
                                requiredString(call, "id"),
                                requiredString(call, "name"),
                                call.getAsJsonObject("arguments")));
                    }
                }
                yield new AssistantMessage(parts, calls);
            }
            case TOOL -> new ToolMessage(
                    requiredString(object, "toolCallId"),
                    requiredString(object, "name"),
                    parts,
                    object.get("error").getAsBoolean());
        };
    }

    private List<ContentPart> readParts(JsonObject object) {
        List<ContentPart> parts = new ArrayList<>();
        for (JsonElement element : requiredArray(object, "parts")) {
            JsonObject part = element.getAsJsonObject();
            String type = requiredString(part, "type");
            parts.add(switch (type) {
                case "text" -> new TextPart(requiredString(part, "text"));
                case "reasoning" -> new ReasoningPart(
                        requiredString(part, "text"),
                        part.has("signature") ? part.get("signature").getAsString() : "");
                case "image" -> new ImagePart(
                        requiredString(part, "mediaType"),
                        Base64.getDecoder().decode(requiredString(part, "data")));
                default -> throw new IllegalArgumentException("Unknown content part: " + type);
            });
        }
        return parts;
    }

    private static String requiredString(JsonObject object, String name) {
        if (!object.has(name) || object.get(name).isJsonNull()) {
            throw new IllegalArgumentException("Missing " + name);
        }
        return object.get(name).getAsString();
    }

    private static JsonArray requiredArray(JsonObject object, String name) {
        if (!object.has(name)) {
            throw new IllegalArgumentException("Missing " + name);
        }
        return object.getAsJsonArray(name);
    }
}
