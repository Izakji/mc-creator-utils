package com.izakji.agent.skill;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loaded skills indexed by id.
 *
 * @author Izakji
 */
public final class SkillRegistry {
    private final Map<String, Skill> skills = new LinkedHashMap<>();

    public void register(Skill skill) {
        if (skills.putIfAbsent(skill.id(), skill) != null) {
            throw new IllegalArgumentException("Skill already registered: " + skill.id());
        }
    }

    public Optional<Skill> find(String id) {
        return Optional.ofNullable(skills.get(id));
    }

    public List<Skill> all() {
        return List.copyOf(skills.values());
    }

    public String indexPrompt() {
        return skills.values().stream()
                .map(skill -> "- " + skill.id() + ": " + skill.name() + " — " + skill.description())
                .reduce("", (left, right) -> left + right + "\n");
    }
}
