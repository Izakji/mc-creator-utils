package com.izakji.mccreatorutils.client.agent.context;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Client-thread block inspection shared by the block tool.
 *
 * @author Izakji
 */
final class BlockInfoReader {
    private BlockInfoReader() {}

    static String build(String blockId) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Level level = player == null ? minecraft.level : player.level();
        if (level == null) {
            throw new IllegalStateException("No world is loaded.");
        }
        BlockState state;
        BlockPos pos = null;
        if (blockId != null && !blockId.isBlank()) {
            ResourceLocation id = ResourceLocation.tryParse(blockId);
            Block block = id == null ? null : BuiltInRegistries.BLOCK.get(id);
            if (block == null) {
                throw new IllegalStateException("Unknown block: " + blockId);
            }
            state = block.defaultBlockState();
        } else {
            var hit = minecraft.hitResult;
            if (!(hit instanceof BlockHitResult blockHit)) {
                throw new IllegalStateException("No block is under the crosshair.");
            }
            pos = blockHit.getBlockPos();
            state = level.getBlockState(pos);
        }
        StringBuilder text = new StringBuilder();
        text.append("Block: ")
                .append(BuiltInRegistries.BLOCK.getKey(state.getBlock()))
                .append(" (")
                .append(state.getBlock().getName().getString())
                .append(")\n");
        if (pos != null) {
            text.append("Position: ")
                    .append(pos.getX())
                    .append(' ')
                    .append(pos.getY())
                    .append(' ')
                    .append(pos.getZ())
                    .append('\n');
            text.append("Hardness: ").append(state.getDestroySpeed(level, pos)).append('\n');
        } else {
            text.append("Default hardness: ")
                    .append(state.getBlock().defaultDestroyTime())
                    .append('\n');
        }
        if (!state.getProperties().isEmpty()) {
            text.append("Properties:");
            for (Property<?> property : state.getProperties()) {
                text.append(' ').append(property.getName()).append('=').append(state.getValue(property));
            }
            text.append('\n');
        }
        if (pos != null && level.getBlockEntity(pos) != null) {
            text.append("Block entity: ")
                    .append(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(
                            level.getBlockEntity(pos).getType()))
                    .append('\n');
        }
        return text.toString();
    }
}
