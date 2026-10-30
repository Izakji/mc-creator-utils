package com.izakji.agent.skill;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Loads SKILL.md files from a skills directory.
 *
 * @author Izakji
 */
public final class SkillLoader {
    public List<Skill> load(Path root) throws IOException {
        if (root == null || !Files.isDirectory(root)) {
            return List.of();
        }
        List<Skill> skills = new ArrayList<>();
        try (Stream<Path> dirs = Files.list(root)) {
            dirs.filter(Files::isDirectory).forEach(dir -> loadOne(dir).ifPresent(skills::add));
        }
        return List.copyOf(skills);
    }

    public Optional<Skill> loadOne(Path skillDirectory) {
        Path file = skillDirectory.resolve("SKILL.md");
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return parse(skillDirectory.getFileName().toString(), Files.readString(file), file);
        } catch (IOException failure) {
            return Optional.empty();
        }
    }

    private Optional<Skill> parse(String id, String content, Path source) {
        String normalized = content.replace("\r\n", "\n");
        String[] lines = normalized.split("\n", -1);
        if (lines.length == 0 || !"---".equals(lines[0].strip())) {
            return Optional.empty();
        }
        int end = -1;
        for (int index = 1; index < lines.length; index++) {
            if ("---".equals(lines[index].strip())) {
                end = index;
                break;
            }
        }
        if (end < 0) {
            return Optional.empty();
        }
        String name = "";
        String description = "";
        List<String> allowed = List.of();
        for (int index = 1; index < end; index++) {
            String line = lines[index].strip();
            int colon = line.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String key = line.substring(0, colon).strip();
            String value = line.substring(colon + 1).strip();
            if ("name".equals(key)) {
                name = value;
            } else if ("description".equals(key)) {
                description = value;
            } else if ("allowed-tools".equals(key)) {
                String trimmed = value.startsWith("[") && value.endsWith("]")
                        ? value.substring(1, value.length() - 1)
                        : value;
                allowed = Arrays.stream(trimmed.split(","))
                        .map(String::strip)
                        .filter(part -> !part.isEmpty())
                        .toList();
            }
        }
        if (name.isBlank() || description.isBlank()) {
            return Optional.empty();
        }
        String body = String.join("\n", Arrays.copyOfRange(lines, end + 1, lines.length)).strip();
        return Optional.of(new Skill(id, name, description, allowed, body, source));
    }
}
