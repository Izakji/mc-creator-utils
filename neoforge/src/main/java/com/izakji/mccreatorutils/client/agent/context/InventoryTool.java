package com.izakji.mccreatorutils.client.agent.context;

import com.google.gson.JsonObject;
import com.izakji.agent.api.AgentTool;
import com.izakji.agent.api.ToolContext;
import com.izakji.agent.api.ToolInvocation;
import com.izakji.agent.api.ToolResult;
import com.izakji.agent.api.ToolRisk;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * Read-only player inventory tool.
 *
 * @author Izakji
 */
public final class InventoryTool implements AgentTool {
    private static final int MAX_ENTRIES = 200;

    @Override
    public String name() {
        return "inventory";
    }

    @Override
    public String description() {
        return "List non-empty player inventory slots with ids, counts, names, and damage";
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
        return ClientThread.supply(() -> {
                    LocalPlayer player = Minecraft.getInstance().player;
                    if (player == null) {
                        throw new IllegalStateException("No player is connected.");
                    }
                    var inventory = player.getInventory();
                    List<InventoryEntry> entries = new ArrayList<>();
                    for (int slot = 0; slot < inventory.getContainerSize() && entries.size() < MAX_ENTRIES; slot++) {
                        ItemStack stack = inventory.getItem(slot);
                        if (stack.isEmpty()) {
                            continue;
                        }
                        entries.add(new InventoryEntry(
                                slot,
                                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                                stack.getCount(),
                                stack.getHoverName().getString(),
                                stack.getDamageValue()));
                    }
                    return InventoryReport.format(entries);
                })
                .thenApply(text -> new ToolResult(invocation.callId(), text, false, List.of()))
                .exceptionally(failure ->
                        new ToolResult(invocation.callId(), "Could not read the inventory.", true, List.of()));
    }
}
