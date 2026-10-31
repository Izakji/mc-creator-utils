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
 * Read-only block details for the crosshair target or one block id.
 *
 * @author Izakji
 */
public final class BlockInfoTool implements AgentTool {

    @Override
    public String name() {
        return "block_info";
    }

    @Override
    public String description() {
        return "Inspect the block under the crosshair or a block id";
    }

    @Override
    public JsonObject inputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject properties = new JsonObject();
        JsonObject block = new JsonObject();
        block.addProperty("type", "string");
        properties.add("block", block);
        schema.add("properties", properties);
        return schema;
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.READ_ONLY;
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolInvocation invocation, ToolContext context) {
        String blockId =
                invocation.arguments() != null && invocation.arguments().has("block")
                        ? invocation.arguments().get("block").getAsString()
                        : "";
        return ClientThread.supply(() -> BlockInfoReader.build(blockId))
                .thenApply(text -> new ToolResult(invocation.callId(), text, false, List.of()))
                .exceptionally(failure -> new ToolResult(
                        invocation.callId(),
                        failure.getMessage() == null ? "Could not inspect the block." : failure.getMessage(),
                        true,
                        List.of()));
    }
}
