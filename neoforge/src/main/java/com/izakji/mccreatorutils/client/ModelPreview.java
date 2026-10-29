package com.izakji.mccreatorutils.client;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.ItemTransform;
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

    private static final float TWO_PI = (float) (Math.PI * 2.0);
    private static final float SPIN_RADIANS_PER_TICK = (float) Math.toRadians(-0.75f);
    private static final float DRAG_RADIANS_PER_PIXEL = (float) Math.toRadians(0.45f);
    private static final float MIN_ZOOM = 0.25f;
    private static final float MAX_ZOOM = 3.0f;

    private final Kind kind;
    private final Quaternionf orientation = new Quaternionf();
    private ItemStack itemStack = ItemStack.EMPTY;
    private LivingEntity entity;
    private float spinAngle;
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
                spinAngle = (spinAngle + SPIN_RADIANS_PER_TICK) % TWO_PI;
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
        orientation.identity();
        spinAngle = 0.0f;
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
        float dx = event.x - lastMouseX;
        float dy = event.y - lastMouseY;
        if (dx != 0.0f) {
            orientation.premul(new Quaternionf().rotateY(-dx * DRAG_RADIANS_PER_PIXEL));
        }
        if (dy != 0.0f) {
            orientation.premul(new Quaternionf().rotateX(dy * DRAG_RADIANS_PER_PIXEL));
        }
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

        int scissorX = Math.round(getPositionX());
        int scissorY = Math.round(getPositionY());
        int scissorRight = Math.round(getPositionX() + width);
        int scissorBottom = Math.round(getPositionY() + height);
        Quaternionf rotation = new Quaternionf().rotateY(spinAngle).mul(orientation);
        context.graphics.enableScissor(scissorX, scissorY, scissorRight, scissorBottom);
        try {
            if (kind == Kind.ITEM && !itemStack.isEmpty()) {
                float maxScale = Math.min(width, height) * 0.80f;
                float scale = Math.min(Math.min(width, height) * 0.22f * zoom, maxScale);
                drawItemPreview(
                        context.graphics,
                        itemStack,
                        getPositionX() + width / 2f,
                        getPositionY() + height / 2f,
                        scale,
                        rotation);
            } else if (kind == Kind.MOB && entity != null) {
                drawEntityPreview(
                        context.graphics,
                        entity,
                        getPositionX() + width / 2f,
                        getPositionY() + height / 2f,
                        width * 0.90f,
                        height * 0.90f,
                        zoom,
                        rotation);
            }
        } finally {
            context.graphics.disableScissor();
        }
    }

    public static void drawItemPreview(
            GuiGraphics graphics, ItemStack stack, float centerX, float centerY, float scale, Quaternionf rotation) {
        if (stack.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ItemRenderer itemRenderer = minecraft.getItemRenderer();
        BakedModel model = itemRenderer.getModel(stack, minecraft.level, null, 0);
        ItemTransform groundTransform = model.getTransforms().getTransform(ItemDisplayContext.GROUND);
        Vector3f pivot = groundTransform.translation;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(centerX, centerY, 100.0f);
        pose.scale(scale, -scale, scale);
        pose.mulPose(rotation);
        // The renderer centers the model at the display transform's translation;
        // remove that offset before rotating so the model spins around its own center.
        pose.translate(-pivot.x(), -pivot.y(), -pivot.z());
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
            GuiGraphics graphics,
            LivingEntity entity,
            float centerX,
            float centerY,
            float maxWidth,
            float maxHeight,
            float zoom,
            Quaternionf rotation) {
        // Keep the entity facing forward; the user rotation is applied as one camera-independent pose.
        entity.setYRot(180.0f);
        entity.setXRot(0.0f);
        entity.yBodyRot = 180.0f;
        entity.yHeadRot = 180.0f;
        entity.yHeadRotO = 180.0f;
        entity.yRotO = 180.0f;
        entity.xRotO = 0.0f;

        float entityWidth = Math.max(0.1f, entity.getBbWidth());
        float entityHeight = Math.max(0.1f, entity.getBbHeight());
        float fitScale = Math.min(maxWidth / entityWidth, maxHeight / entityHeight);
        float scale = Math.max(1.0f, Math.min(fitScale * 0.72f * zoom, fitScale * 0.92f));

        // The entity preview applies a Z-axis mirror, so compensate Y/Z to keep the
        // same screen-space drag direction as the item preview.
        Quaternionf entityRotation = new Quaternionf(rotation.x, -rotation.y, -rotation.z, rotation.w);
        Quaternionf poseRotation = entityRotation.mul(new Quaternionf().rotateZ((float) Math.PI));
        InventoryScreen.renderEntityInInventory(
                graphics,
                centerX,
                centerY,
                scale,
                new Vector3f(0.0f, entityHeight / 2.0f, 0.0f),
                poseRotation,
                null,
                entity);
    }
}
