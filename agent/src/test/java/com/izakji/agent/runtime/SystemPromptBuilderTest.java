package com.izakji.agent.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.izakji.agent.config.TestSettings;
import com.izakji.agent.skill.Skill;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class SystemPromptBuilderTest {

    @Test
    void loadsAgentsMarkdownAndTreatsMissingFileAsEmpty() throws Exception {
        Path file = Files.createTempFile("AGENTS", ".md");
        Files.writeString(file, "Never break the build.");
        InstructionsLoader loader = new InstructionsLoader();
        assertEquals("Never break the build.", loader.load(file));
        assertTrue(loader.load(file.resolveSibling("missing.md")).isEmpty());
    }

    @Test
    void includesInstructionsSkillIndexAndSnapshots() {
        Skill skill = new Skill(
                "plains",
                "Plains",
                "Answer plains questions",
                List.of("game_snapshot"),
                "body",
                Path.of("SKILL.md"));
        String prompt = new SystemPromptBuilder()
                .build(
                        TestSettings.settings(),
                        List.of(skill),
                        "Always be concise.",
                        List.of("player=Steve", "fps=60"));
        assertTrue(prompt.contains("Always be concise."));
        assertTrue(prompt.contains("Plains"));
        assertTrue(prompt.contains("game_snapshot"));
        assertTrue(prompt.contains("player=Steve"));
        assertTrue(prompt.contains("fps=60"));
        assertTrue(prompt.contains("untrusted") || prompt.contains("tool output"));
    }
}
