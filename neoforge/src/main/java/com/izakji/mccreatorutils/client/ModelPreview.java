package com.izakji.mccreatorutils.client;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Slowly spinning, draggable, zoomable item or mob preview.
 *
 * @author Izakji
 */
public final class ModelPreview extends UIElement {
    public enum Kind {
        ITEM,
        MOB
    }

    private static final float SPIN_DEGREES_PER_TICK = 0.75f;
    private static final float MIN_ZOOM = 0.25f;
    private static final float MAX_ZOOM = 3.0f;

    private final Kind kind;
    private ItemStack itemStack = ItemStack.EMPTY;
    private LivingEntity entity;
    private float yaw;
    private float pitch;
    private float zoom = 1.0f;
    private boolean spinning = true;
    private boolean dragging;
    private float lastMouseX;
    private float lastMouseY;

    public ModelPreview(Kind kind) {
        this.kind = kind;
        layout(layout -> layout.widthPercent(100).height(160));
        style(style -> style.background(ColorPattern.WHITE.rectTexture()));
        addEventListener(UIEvents.TICK, event -> {
            if (spinning && !dragging) {
                yaw = (yaw + SPIN_DEGREES_PER_TICK) % 360.0f;
            }
        });
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.MOUSE_MOVE, this::onMouseMove);
        addEventListener(UIEvents.MOUSE_UP, event -> dragging = false);
        addEventListener(UIEvents.MOUSE_WHEEL, this::onMouseWheel);
    }

    public void setSelection(ResourceLocation selection) {
        itemStack = ItemStack.EMPTY;
        entity = null;
        yaw = 0.0f;
        pitch = 0.0f;
        zoom = 1.0f;
        spinning = true;
        dragging = false;
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

    private void onMouseDown(UIEvent event) {
        if (event.button != 0) {
            return;
        }
        spinning = false;
        dragging = true;
        lastMouseX = event.x;
        lastMouseY = event.y;
    }

    private void onMouseMove(UIEvent event) {
        if (!dragging) {
            return;
        }
        yaw = (yaw + (event.x - lastMouseX) * 0.6f) % 360.0f;
        pitch = Mth.clamp(pitch + (event.y - lastMouseY) * 0.6f, -80.0f, 80.0f);
        lastMouseX = event.x;
        lastMouseY = event.y;
    }

    private void onMouseWheel(UIEvent event) {
        float factor = event.deltaY < 0 ? 1.1f : 1.0f / 1.1f;
        zoom = Mth.clamp(zoom * factor, MIN_ZOOM, MAX_ZOOM);
        event.stopPropagation();
    }

    @Override
    public void drawContents(GUIContext context) {
        float width = getSizeWidth();
        float height = getSizeHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        if (kind == Kind.ITEM && !itemStack.isEmpty()) {
            drawItemPreview(
                    context.graphics,
                    itemStack,
                    getPositionX() + width / 2f,
                    getPositionY() + height / 2f,
                    Math.min(width, height) * 0.55f * zoom,
                    -yaw,
                    pitch);
        } else if (kind == Kind.MOB && entity != null) {
            float halfWidth = width * 0.45f * zoom;
            float halfHeight = height * 0.45f * zoom;
            int x1 = Math.round(getPositionX() + width / 2f - halfWidth);
            int y1 = Math.round(getPositionY() + height / 2f - halfHeight);
            int x2 = Math.round(getPositionX() + width / 2f + halfWidth);
            int y2 = Math.round(getPositionY() + height / 2f + halfHeight);
            drawEntityPreview(context.graphics, entity, x1, y1, x2, y2, yaw, pitch);
        }
    }

    public static void drawItemPreview(
            GuiGraphics graphics, ItemStack stack, float centerX, float centerY, float scale, float yaw, float pitch) {
        if (stack.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ItemRenderer itemRenderer = minecraft.getItemRenderer();
        BakedModel model = itemRenderer.getModel(stack, minecraft.level, null, 0);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(centerX, centerY, 100.0f);
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        float groundScale = model.getTransforms()
                .getTransform(ItemDisplayContext.GROUND)
                .scale
                .y();
        pose.translate(0.0f, 0.25f * groundScale, 0.0f);
        Lighting.setupFor3DItems();
        itemRenderer.render(
                stack,
                ItemDisplayContext.GROUND,
                false,
                pose,
                graphics.bufferSource(),
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                model);
        pose.popPose();
        graphics.flush();
        Lighting.setupFor3DItems();
    }

    public static void drawEntityPreview(
            GuiGraphics graphics, LivingEntity entity, int x1, int y1, int x2, int y2, float yaw, float pitch) {
        entity.setYRot(180.0f + yaw);
        entity.setXRot(pitch);
        entity.yBodyRot = 180.0f + yaw;
        entity.yHeadRot = 180.0f + yaw;
        entity.yHeadRotO = 180.0f + yaw;

        float maxDimension = Math.max(entity.getBbWidth(), entity.getBbHeight());
        int width = x2 - x1;
        int height = y2 - y1;
        int scale = Math.max(
                4, Math.min(80, Math.round(Math.min(width, height) * 0.55f * 1.8f / Math.max(1.8f, maxDimension))));
        float centerX = (x1 + x2) / 2.0f;
        float centerY = (y1 + y2) / 2.0f;
        Quaternionf poseRotation = new Quaternionf()
                .rotateZ((float) Math.PI)
                .rotateY((float) Math.toRadians(yaw))
                .rotateX((float) Math.toRadians(pitch));
        Quaternionf cameraRotation = new Quaternionf().rotateX((float) Math.toRadians(pitch));
        InventoryScreen.renderEntityInInventory(
                graphics,
                centerX,
                centerY,
                scale,
                new Vector3f(0.0f, entity.getBbHeight() / 2.0f, 0.0f),
                poseRotation,
                cameraRotation,
                entity);
    }
}
