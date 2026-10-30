package com.izakji.agent.conversation;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * One JSON file per conversation.
 *
 * @author Izakji
 */
public final class FileConversationStore implements ConversationStore {
    private final Path root;
    private final ConversationJson json;

    public FileConversationStore(Path root, Gson gson) {
        this.root = root.toAbsolutePath();
        this.json = new ConversationJson(gson);
    }

    @Override
    public Conversation create(String title, String profileId) {
        Instant now = Instant.now();
        return new Conversation(UUID.randomUUID().toString(), title, now, now, profileId, List.of(), Set.of());
    }

    @Override
    public Optional<Conversation> load(String id) {
        Path file = path(id);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(json.read(Files.readString(file)));
        } catch (Exception failure) {
            quarantine(file);
            return Optional.empty();
        }
    }

    @Override
    public void save(Conversation conversation) {
        try {
            Files.createDirectories(root);
            Path target = path(conversation.id());
            Path temp = Files.createTempFile(root, conversation.id(), ".tmp");
            Files.writeString(temp, json.write(conversation));
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicFailure) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException failure) {
            throw new IllegalStateException("Could not save conversation", failure);
        }
    }

    @Override
    public List<ConversationSummary> list() {
        List<ConversationSummary> summaries = new ArrayList<>();
        if (!Files.isDirectory(root)) {
            return summaries;
        }
        try (Stream<Path> files = Files.list(root)) {
            files.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .forEach(path -> load(idOf(path))
                            .ifPresent(conversation -> summaries.add(new ConversationSummary(
                                    conversation.id(), conversation.title(), conversation.updated()))));
        } catch (IOException failure) {
            return List.of();
        }
        summaries.sort(Comparator.comparing(ConversationSummary::updated).reversed());
        return List.copyOf(summaries);
    }

    @Override
    public void delete(String id) {
        try {
            Files.deleteIfExists(path(id));
        } catch (IOException failure) {
            throw new IllegalStateException("Could not delete conversation", failure);
        }
    }

    @Override
    public void rename(String id, String title) {
        load(id).ifPresent(conversation -> save(conversation.withTitle(title, Instant.now())));
    }

    private Path path(String id) {
        return root.resolve(id + ".json");
    }

    private static String idOf(Path file) {
        String name = file.getFileName().toString();
        return name.substring(0, name.length() - ".json".length());
    }

    private void quarantine(Path file) {
        try {
            Files.move(
                    file,
                    file.resolveSibling(file.getFileName() + ".corrupt-" + System.currentTimeMillis()),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // A failed quarantine must not make loading other conversations fail.
        }
    }
}
