package com.izakji.mccreatorutils.client.agent.context;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client-thread entity inspection shared by the entity tool.
 *
 * @author Izakji
 */
final class EntityInfoReader {
    private EntityInfoReader() {}

    static String build() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            throw new IllegalStateException("No player is connected.");
        }
        Entity target = target(minecraft, player);
        if (target == null) {
            throw new IllegalStateException("No entity is close enough or visible.");
        }
        StringBuilder text = new StringBuilder();
        text.append("Entity: ")
                .append(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()))
                .append(" (")
                .append(target.getName().getString())
                .append(")\n");
        text.append("UUID: ").append(target.getUUID()).append('\n');
        text.append("Position: ")
                .append(String.format(Locale.ROOT, "%.2f %.2f %.2f", target.getX(), target.getY(), target.getZ()))
                .append(" distance ")
                .append(String.format(Locale.ROOT, "%.2f", target.distanceTo(player)))
                .append('\n');
        if (target instanceof LivingEntity living) {
            text.append("Health: ")
                    .append(living.getHealth())
                    .append('/')
                    .append(living.getMaxHealth())
                    .append('\n');
            text.append("Main hand: ")
                    .append(BuiltInRegistries.ITEM.getKey(
                            living.getMainHandItem().getItem()))
                    .append('\n');
            text.append("Off hand: ")
                    .append(BuiltInRegistries.ITEM.getKey(
                            living.getOffhandItem().getItem()))
                    .append('\n');
            if (!living.getActiveEffects().isEmpty()) {
                text.append("Effects:");
                for (var effect : living.getActiveEffects()) {
                    text.append(' ')
                            .append(effect.getEffect()
                                    .unwrapKey()
                                    .map(key -> key.location().toString())
                                    .orElse("unknown"))
                            .append('(')
                            .append(effect.getDuration())
                            .append(')');
                }
                text.append('\n');
            }
        }
        return text.toString();
    }

    private static Entity target(Minecraft minecraft, LocalPlayer player) {
        if (minecraft.hitResult instanceof EntityHitResult entityHit) {
            return entityHit.getEntity();
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity :
                player.level().getEntities(player, player.getBoundingBox().inflate(8.0))) {
            if (!entity.isAlive() || !player.hasLineOfSight(entity)) {
                continue;
            }
            Vec3 offset = entity.getEyePosition().subtract(eye);
            double distance = offset.length();
            if (distance <= 0.01 || distance > 8.0) {
                continue;
            }
            if (offset.normalize().dot(look) < Math.cos(Math.toRadians(30.0))) {
                continue;
            }
            if (distance < bestDistance) {
                best = entity;
                bestDistance = distance;
            }
        }
        return best;
    }
}
