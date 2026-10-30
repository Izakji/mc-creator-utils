package com.izakji.agent.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class SkillLoaderTest {

    @Test
    void loadsFrontmatterAndBody() throws Exception {
        Path root = Files.createTempDirectory("skills");
        Path skill = root.resolve("plains/SKILL.md");
        Files.createDirectories(skill.getParent());
        Files.writeString(skill, """
                ---
                name: Plains
                description: Biome questions
                allowed-tools: [game_snapshot, biome_info]
                ---
                Use biome_info.
                """);
        List<Skill> skills = new SkillLoader().load(root);
        assertEquals(1, skills.size());
        assertEquals("plains", skills.get(0).id());
        assertEquals("Plains", skills.get(0).name());
        assertEquals(List.of("game_snapshot", "biome_info"), skills.get(0).allowedTools());
        assertTrue(skills.get(0).body().contains("biome_info"));
    }

    @Test
    void skipsInvalidFrontmatterWithoutFailingOthers() throws Exception {
        Path root = Files.createTempDirectory("skills");
        Path good = root.resolve("good/SKILL.md");
        Path bad = root.resolve("bad/SKILL.md");
        Files.createDirectories(good.getParent());
        Files.createDirectories(bad.getParent());
        Files.writeString(good, "---\nname: Good\ndescription: Works\n---\nbody\n");
        Files.writeString(bad, "---\nname: Bad\n---\nbody\n");
        assertEquals(
                List.of("good"),
                new SkillLoader().load(root).stream().map(Skill::id).toList());
    }
}
