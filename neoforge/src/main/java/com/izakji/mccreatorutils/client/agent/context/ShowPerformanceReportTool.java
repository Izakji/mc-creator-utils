package com.izakji.mccreatorutils.client.agent.context;

import com.google.gson.JsonObject;
import com.izakji.agent.api.AgentTool;
import com.izakji.agent.api.ToolContext;
import com.izakji.agent.api.ToolInvocation;
import com.izakji.agent.api.ToolResult;
import com.izakji.agent.api.ToolRisk;
import com.izakji.mccreatorutils.client.agent.AgentReportScreen;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Builds and opens the visual performance report.
 *
 * @author Izakji
 */
public final class ShowPerformanceReportTool implements AgentTool {
    private final ClientLogReader reader;

    public ShowPerformanceReportTool(ClientLogReader reader) {
        this.reader = reader;
    }

    @Override
    public String name() {
        return "show_performance_report";
    }

    @Override
    public String description() {
        return "Open a visual report with FPS, TPS, MSPT, heap, ping charts, health findings, and log errors";
    }

    @Override
    public JsonObject inputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", new JsonObject());
        return schema;
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.READ_ONLY;
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolInvocation invocation, ToolContext context) {
        return PerformanceReportSource.build(reader)
                .thenCompose(data -> ClientThread.supply(() -> {
                    AgentReportScreen.open(data, () -> PerformanceReportSource.build(reader));
                    return data;
                }))
                .thenApply(data -> new ToolResult(invocation.callId(), data.summaryText(), false, List.of()))
                .exceptionally(failure -> {
                    Throwable cause = failure.getCause() == null ? failure : failure.getCause();
                    String message =
                            cause.getMessage() == null ? "Could not build the performance report." : cause.getMessage();
                    return new ToolResult(invocation.callId(), message, true, List.of());
                });
    }
}
