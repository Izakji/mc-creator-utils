package com.izakji.mccreatorutils.client.agent.context;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import com.izakji.agent.api.AgentTool;
import com.izakji.agent.api.ToolContext;
import com.izakji.agent.api.ToolInvocation;
import com.izakji.agent.api.ToolResult;
import com.izakji.agent.api.ToolRisk;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class CommandRiskClassifierTest {

    @Test
    void escalatesOnlyTheCommandTool() {
        CommandRiskClassifier classifier = new CommandRiskClassifier();
        RunCommandTool tool = new RunCommandTool();
        assertEquals(
                ToolRisk.READ_ONLY,
                classifier.classify(tool, invocation("locate structure minecraft:stronghold"), ToolRisk.READ_ONLY));
        assertEquals(ToolRisk.MUTATING, classifier.classify(tool, invocation("tp Steve 0 64 0"), ToolRisk.READ_ONLY));
        assertEquals(
                ToolRisk.READ_ONLY,
                classifier.classify(new OtherTool(), invocation("tp Steve 0 64 0"), ToolRisk.READ_ONLY));
    }

    private static ToolInvocation invocation(String command) {
        JsonObject arguments = new JsonObject();
        arguments.addProperty("command", command);
        return new ToolInvocation("call", "run_command", arguments);
    }

    private static final class OtherTool implements AgentTool {
        @Override
        public String name() {
            return "other";
        }

        @Override
        public String description() {
            return "other";
        }

        @Override
        public JsonObject inputSchema() {
            return new JsonObject();
        }

        @Override
        public ToolRisk risk() {
            return ToolRisk.READ_ONLY;
        }

        @Override
        public CompletableFuture<ToolResult> execute(ToolInvocation invocation, ToolContext context) {
            return CompletableFuture.completedFuture(new ToolResult("call", "", false, java.util.List.of()));
        }
    }
}
