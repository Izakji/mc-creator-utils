package com.izakji.agent.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PackLoaderTest {

    @Test
    void loadsPackSkillsAndRejectsEscapingPaths() throws Exception {
        Path root = Files.createTempDirectory("packs");
        Path pack = root.resolve("example");
        Files.createDirectories(pack.resolve("skills/plains"));
        Files.writeString(pack.resolve("skills/plains/SKILL.md"), """
                ---
                name: Plains
                description: Biome questions
                ---
                body
                """);
        Files.writeString(pack.resolve("pack.json"), """
                {"id":"example","name":"Example","version":"1",
                 "skills":["skills/plains"],"mcpServers":[]}
                """);
        Path bad = root.resolve("bad");
        Files.createDirectories(bad);
        Files.writeString(bad.resolve("pack.json"), """
                {"id":"bad","name":"Bad","version":"1",
                 "skills":["../../../etc"],"mcpServers":[]}
                """);

        PackLoadResult result = new PackLoader().load(root);
        assertEquals(1, result.packs().size());
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("bad")));
    }
}
