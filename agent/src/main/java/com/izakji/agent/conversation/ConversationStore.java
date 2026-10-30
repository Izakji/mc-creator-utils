package com.izakji.agent.conversation;

import java.util.List;
import java.util.Optional;

/**
 * Persistent conversation storage.
 *
 * @author Izakji
 */
public interface ConversationStore {
    Conversation create(String title, String profileId);

    Optional<Conversation> load(String id);

    void save(Conversation conversation);

    List<ConversationSummary> list();

    void delete(String id);

    void rename(String id, String title);
}
