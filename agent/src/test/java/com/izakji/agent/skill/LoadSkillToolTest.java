package com.izakji.agent.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.izakji.agent.api.ToolInvocation;
import com.izakji.agent.api.ToolRisk;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class LoadSkillToolTest {

    @Test
    void returnsTheSkillBodyAndRejectsUnknownIds() {
        SkillRegistry registry = new SkillRegistry();
        registry.register(new Skill("plains", "Plains", "Biome questions", List.of(), "body", Path.of("SKILL.md")));
        LoadSkillTool tool = new LoadSkillTool(registry);
        assertEquals(ToolRisk.READ_ONLY, tool.risk());
        JsonObject args = new JsonObject();
        args.addProperty("id", "plains");
        assertEquals(
                "body",
                tool.execute(new ToolInvocation("c", "load_skill", args), null)
                        .join()
                        .content());
        args.addProperty("id", "missing");
        assertTrue(tool.execute(new ToolInvocation("c", "load_skill", args), null)
                .join()
                .error());
    }
}
