package com.izakji.agent.skill;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Loaded skill instructions.
 *
 * @param id stable skill id
 * @param name display name
 * @param description one-line trigger description
 * @param allowedTools advisory tool names
 * @param body Markdown body
 * @param source source file
 * @author Izakji
 */
public record Skill(String id, String name, String description, List<String> allowedTools, String body, Path source) {

    public Skill {
        id = Objects.requireNonNull(id, "id");
        name = Objects.requireNonNull(name, "name");
        description = description == null ? "" : description;
        allowedTools = List.copyOf(Objects.requireNonNullElse(allowedTools, List.of()));
        body = Objects.requireNonNullElse(body, "");
        Objects.requireNonNull(source, "source");
    }
}
