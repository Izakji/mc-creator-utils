package com.izakji.mccreatorutils.client;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Slowly spinning item or mob model preview.
 *
 * @author Izakji
 */
public final class ModelPreview extends UIElement {
    public enum Kind {
        ITEM,
        MOB
    }

    private static final float SPIN_DEGREES_PER_TICK = 0.75f;

    private final Kind kind;
    private ItemStack itemStack = ItemStack.EMPTY;
    private LivingEntity entity;
    private float angle;

    public ModelPreview(Kind kind) {
        this.kind = kind;
        layout(layout -> layout.widthPercent(100).height(60));
        addEventListener(UIEvents.TICK, event -> angle = (angle + SPIN_DEGREES_PER_TICK) % 360f);
    }

    public void setSelection(ResourceLocation selection) {
        itemStack = ItemStack.EMPTY;
        entity = null;
        if (selection == null) {
            return;
        }

        if (kind == Kind.ITEM) {
            Item item = BuiltInRegistries.ITEM.get(selection);
            if (item != null) {
                itemStack = new ItemStack(item);
            }
            return;
        }

        Level level = Minecraft.getInstance().level;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(selection);
        if (level == null || type == null) {
            return;
        }
        try {
            Entity created = type.create(level);
            if (created instanceof LivingEntity living) {
                living.setPos(0, 0, 0);
                living.setYRot(0);
                living.setXRot(0);
                entity = living;
            }
        } catch (RuntimeException ignored) {
            entity = null;
        }
    }

    @Override
    public void drawContents(GUIContext context) {
        float width = getSizeWidth();
        float height = getSizeHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        if (kind == Kind.ITEM && !itemStack.isEmpty()) {
            drawItem(context, width, height);
        } else if (kind == Kind.MOB && entity != null) {
            drawEntity(context, width, height);
        }
    }

    private void drawItem(GUIContext context, float width, float height) {
        float scale = Math.min(width, height) / 16f;
        PoseStack pose = context.graphics.pose();
        pose.pushPose();
        pose.translate(getPositionX() + width / 2f, getPositionY() + height / 2f, 100.0f);
        pose.mulPose(Axis.ZP.rotationDegrees(-angle));
        pose.scale(scale, scale, 1.0f);
        pose.translate(-8.0f, -8.0f, 0.0f);
        context.graphics.renderItem(itemStack, 0, 0);
        pose.popPose();
        context.graphics.flush();
    }

    private void drawEntity(GUIContext context, float width, float height) {
        int x1 = Math.round(getPositionX());
        int y1 = Math.round(getPositionY());
        int x2 = Math.round(getPositionX() + width);
        int y2 = Math.round(getPositionY() + height);
        float maxDimension = Math.max(entity.getBbWidth(), entity.getBbHeight());
        int scale = Math.max(
                4, Math.min(40, Math.round(Math.min(width, height) * 0.55f * 1.8f / Math.max(1.8f, maxDimension))));
        InventoryScreen.renderEntityInInventoryFollowsAngle(
                context.graphics, x1, y1, x2, y2, scale, 0.0f, angle / 40f, 0.0f, entity);
    }
}
