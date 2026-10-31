package com.izakji.mccreatorutils.client.agent.context;

import com.izakji.agent.api.ToolContext;
import com.izakji.agent.runtime.ContextProvider;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.fml.ModList;

/**
 * Captures a bounded live game snapshot on the client thread.
 *
 * @author Izakji
 */
public final class GameSnapshotProvider implements ContextProvider {
    private static final int MAX_LENGTH = 2048;

    @Override
    public String id() {
        return "game_snapshot";
    }

    @Override
    public CompletableFuture<String> snapshot(ToolContext context) {
        return capture();
    }

    /**
     * Refreshes the client snapshot.
     *
     * @return bounded snapshot text
     */
    public CompletableFuture<String> capture() {
        CompletableFuture<String> future = new CompletableFuture<>();
        Minecraft.getInstance().execute(() -> {
            try {
                future.complete(build(Minecraft.getInstance()));
            } catch (RuntimeException failure) {
                future.complete(
                        "Game snapshot unavailable: " + failure.getClass().getSimpleName());
            }
        });
        return future;
    }

    private static String build(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return "No player is connected.";
        }
        Level level = player.level();
        BlockPos position = player.blockPosition();
        HitResult hit = minecraft.hitResult;
        String biome = level.getBiome(position)
                .unwrapKey()
                .map(key -> key.location().toString())
                .orElse("unknown");
        String held = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()) + " x"
                + player.getMainHandItem().getCount();
        String target = target(level, hit);
        String screen =
                minecraft.screen == null ? "none" : minecraft.screen.getClass().getSimpleName();
        GameSnapshot snapshot = new GameSnapshot(
                player.getName().getString(),
                gameMode(player),
                player.getHealth(),
                player.getMaxHealth(),
                player.getFoodData().getFoodLevel(),
                player.experienceLevel,
                player.getArmorValue(),
                held,
                level.dimension().location().toString(),
                biome,
                position.getX(),
                position.getY(),
                position.getZ(),
                level.getDayTime(),
                level.isThundering() ? "thunder" : level.isRaining() ? "rain" : "clear",
                target,
                screen,
                minecraft.getFps(),
                ping(minecraft, player),
                Runtime.getRuntime().totalMemory() / (1024 * 1024),
                Runtime.getRuntime().maxMemory() / (1024 * 1024),
                minecraft.options.renderDistance().get(),
                ModList.get().size());
        String text = snapshot.format();
        return text.length() <= MAX_LENGTH ? text : text.substring(0, MAX_LENGTH);
    }

    private static String gameMode(LocalPlayer player) {
        if (player.isCreative()) {
            return "creative";
        }
        if (player.isSpectator()) {
            return "spectator";
        }
        return "survival";
    }

    private static int ping(Minecraft minecraft, LocalPlayer player) {
        if (minecraft.getConnection() == null) {
            return 0;
        }
        var info = minecraft.getConnection().getPlayerInfo(player.getUUID());
        return info == null ? 0 : info.getLatency();
    }

    private static String target(Level level, HitResult hit) {
        if (hit instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()) + " \""
                    + entity.getName().getString() + "\"";
        }
        if (hit instanceof BlockHitResult blockHit) {
            BlockState state = level.getBlockState(blockHit.getBlockPos());
            return BuiltInRegistries.BLOCK.getKey(state.getBlock()) + " at "
                    + blockHit.getBlockPos().getX() + " "
                    + blockHit.getBlockPos().getY() + " "
                    + blockHit.getBlockPos().getZ();
        }
        return "none";
    }
}
