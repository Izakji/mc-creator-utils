package com.izakji.mccreatorutils.client.agent.context;

import com.google.gson.JsonObject;
import com.izakji.agent.api.AgentTool;
import com.izakji.agent.api.ToolContext;
import com.izakji.agent.api.ToolInvocation;
import com.izakji.agent.api.ToolResult;
import com.izakji.agent.api.ToolRisk;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Runs a vanilla, modded, or client command and returns captured chat output.
 *
 * @author Izakji
 */
public final class RunCommandTool implements AgentTool, CommandExecutingTool {

    @Override
    public String name() {
        return "run_command";
    }

    @Override
    public String description() {
        return "Run a Minecraft command. Read-only commands such as locate, seed, help, and queries auto-run; "
                + "other commands require approval. Server permissions still apply.";
    }

    @Override
    public JsonObject inputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject properties = new JsonObject();
        JsonObject command = new JsonObject();
        command.addProperty("type", "string");
        properties.add("command", command);
        schema.add("properties", properties);
        com.google.gson.JsonArray required = new com.google.gson.JsonArray();
        required.add("command");
        schema.add("required", required);
        return schema;
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.READ_ONLY;
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolInvocation invocation, ToolContext context) {
        String command = ToolArguments.string(invocation.arguments(), "command", "");
        CompletableFuture<ToolResult> result = ClientThread.supply(() -> {
                    context.cancellationToken().throwIfCancelled();
                    return CommandRunner.run(command, context.cancellationToken()::isCancelled);
                })
                .thenCompose(future -> future)
                .thenApply(value -> new ToolResult(invocation.callId(), format(value), false, List.of()))
                .exceptionally(failure -> {
                    Throwable cause = failure.getCause() == null ? failure : failure.getCause();
                    String message = cause.getMessage() == null ? "Command failed." : cause.getMessage();
                    return new ToolResult(invocation.callId(), message, true, List.of());
                });
        result.whenComplete((ignored, failure) -> {
            if (result.isCancelled()) {
                CommandRunner.cancel();
            }
        });
        return result;
    }

    private static String format(CommandRunner.CommandResult result) {
        StringBuilder report = new StringBuilder();
        report.append("Command: /").append(result.command()).append('\n');
        if (result.lines().isEmpty()) {
            report.append(
                    "No chat output was captured. The command may have executed silently, or the server rejected it without a visible message.");
        } else {
            report.append("Response:\n");
            for (String line : result.lines()) {
                report.append(line).append('\n');
            }
            if (result.droppedLines() > 0) {
                report.append("... ")
                        .append(result.droppedLines())
                        .append(" more lines omitted by the capture limit.\n");
            }
        }
        report.append("Server permissions still apply. Capture may include unrelated system chat.");
        return report.toString();
    }
}
