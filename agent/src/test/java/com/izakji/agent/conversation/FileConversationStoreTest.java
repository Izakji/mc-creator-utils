package com.izakji.agent.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.izakji.agent.api.AssistantMessage;
import com.izakji.agent.api.ImagePart;
import com.izakji.agent.api.ReasoningPart;
import com.izakji.agent.api.TextPart;
import com.izakji.agent.api.ToolCall;
import com.izakji.agent.api.ToolMessage;
import com.izakji.agent.api.UserMessage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class FileConversationStoreTest {

    @Test
    void createsLoadsRenamesListsAndDeletes() throws Exception {
        Path root = Files.createTempDirectory("agent-conversations");
        FileConversationStore store = new FileConversationStore(root, new Gson());
        Conversation created = store.create("New Chat", "main");
        store.save(created.withMessage(
                new UserMessage(List.of(new TextPart("hello"))), Instant.parse("2026-10-30T00:00:00Z")));
        store.rename(created.id(), "Renamed");

        Conversation loaded = store.load(created.id()).orElseThrow();
        assertEquals("Renamed", loaded.title());
        assertEquals(1, loaded.messages().size());
        assertEquals(
                List.of("Renamed"),
                store.list().stream().map(ConversationSummary::title).toList());

        store.delete(created.id());
        assertTrue(store.load(created.id()).isEmpty());
    }

    @Test
    void roundTripsReasoningToolCallsAndImages() throws Exception {
        Path root = Files.createTempDirectory("agent-conversations");
        FileConversationStore store = new FileConversationStore(root, new Gson());
        Conversation created = store.create("Chat", "main");
        Conversation withUser = created.withMessage(new UserMessage(List.of(new TextPart("hi"))), Instant.now());
        ToolCall call = new ToolCall("call-1", "mutate", new JsonObject());
        Conversation withAssistant = withUser.withMessage(
                new AssistantMessage(List.of(new ReasoningPart("why", "sig")), List.of(call)), Instant.now());
        Conversation withTool = withAssistant.withMessage(
                new ToolMessage(
                        "call-1",
                        "mutate",
                        List.of(new TextPart("ok"), new ImagePart("image/png", new byte[] {1, 2})),
                        false),
                Instant.now());
        store.save(withTool);

        Conversation loaded = store.load(created.id()).orElseThrow();
        assertTrue(((AssistantMessage) loaded.messages().get(1)).parts().get(0) instanceof ReasoningPart);
        assertEquals(
                "call-1",
                ((AssistantMessage) loaded.messages().get(1)).toolCalls().get(0).id());
        assertTrue(((ToolMessage) loaded.messages().get(2)).parts().get(1) instanceof ImagePart);
    }

    @Test
    void quarantinesCorruptConversationFiles() throws Exception {
        Path root = Files.createTempDirectory("agent-conversations");
        FileConversationStore store = new FileConversationStore(root, new Gson());
        String id = java.util.UUID.randomUUID().toString();
        Files.writeString(root.resolve(id + ".json"), "{broken");
        assertTrue(store.load(id).isEmpty());
        try (Stream<Path> files = Files.list(root)) {
            assertTrue(files.anyMatch(path -> path.getFileName().toString().contains(".corrupt-")));
        }
    }

    @Test
    void rejectsConversationIdsThatEscapeTheStoreDirectory() throws Exception {
        Path root = Files.createTempDirectory("agent-conversations");
        FileConversationStore store = new FileConversationStore(root, new Gson());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> store.load("../escape"));
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> store.delete("../../escape"));
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> store.save(new Conversation(
                        "../escape", "Bad", Instant.now(), Instant.now(), "main", List.of(), java.util.Set.of())));
    }

    @Test
    void ignoresFilesWhoseStoredIdDoesNotMatchTheirName() throws Exception {
        Path root = Files.createTempDirectory("agent-conversations");
        FileConversationStore store = new FileConversationStore(root, new Gson());
        String fileNameId = java.util.UUID.randomUUID().toString();
        Conversation other = new Conversation(
                java.util.UUID.randomUUID().toString(),
                "Other",
                Instant.now(),
                Instant.now(),
                "main",
                List.of(),
                java.util.Set.of());
        Files.writeString(root.resolve(fileNameId + ".json"), new ConversationJson(new Gson()).write(other));
        assertTrue(store.load(fileNameId).isEmpty());
    }

    @Test
    void repairsUnansweredToolCallsWhenLoading() throws Exception {
        Path root = Files.createTempDirectory("agent-conversations");
        FileConversationStore store = new FileConversationStore(root, new Gson());
        Conversation conversation = store.create("Chat", "main");
        Conversation dangling = conversation.withMessage(
                new com.izakji.agent.api.AssistantMessage(
                        List.of(), List.of(new ToolCall("call-1", "mutate", new JsonObject()))),
                Instant.now());
        Files.writeString(root.resolve(conversation.id() + ".json"), new ConversationJson(new Gson()).write(dangling));

        Conversation loaded = store.load(conversation.id()).orElseThrow();
        assertTrue(loaded.messages().stream()
                .anyMatch(message ->
                        message instanceof ToolMessage tool && tool.toolCallId().equals("call-1") && tool.error()));
    }
}
