package com.izakji.mccreatorutils.client;

import com.izakji.mccreatorutils.tool.ToolId;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Full-size interactive item or mob model preview.
 *
 * @author Izakji
 */
public final class ModelPreviewScreen extends Screen {
    private static final float MIN_ZOOM = 0.25f;
    private static final float MAX_ZOOM = 3.0f;

    private final ModelPreview.Kind kind;
    private final ToolId returnTool;
    private final ItemStack itemStack;
    private final LivingEntity entity;
    private float yaw;
    private float pitch;
    private float zoom = 1.0f;
    private boolean dragging;
    private double lastMouseX;
    private double lastMouseY;

    public ModelPreviewScreen(ModelPreview.Kind kind, ResourceLocation selection, ToolId returnTool) {
        super(Component.translatable("screen.mccreatorutils.model_preview"));
        this.kind = kind;
        this.returnTool = returnTool;

        ItemStack stack = ItemStack.EMPTY;
        LivingEntity living = null;
        if (kind == ModelPreview.Kind.ITEM) {
            Item item = BuiltInRegistries.ITEM.get(selection);
            if (item != null) {
                stack = new ItemStack(item);
            }
        } else {
            Level level = Minecraft.getInstance().level;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(selection);
            if (level != null && type != null) {
                try {
                    Entity created = type.create(level);
                    if (created instanceof LivingEntity candidate) {
                        candidate.setPos(0, 0, 0);
                        living = candidate;
                    }
                } catch (RuntimeException ignored) {
                    living = null;
                }
            }
        }
        itemStack = stack;
        entity = living;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFFFFFFFF);
        if (kind == ModelPreview.Kind.ITEM) {
            ModelPreview.drawItemPreview(
                    graphics,
                    itemStack,
                    width / 2.0f,
                    height / 2.0f,
                    Math.min(width, height) * 0.45f * zoom,
                    yaw,
                    pitch);
        } else if (entity != null) {
            int halfWidth = Math.round(width * 0.3f * zoom);
            int halfHeight = Math.round(height * 0.35f * zoom);
            ModelPreview.drawEntityPreview(
                    graphics,
                    entity,
                    width / 2 - halfWidth,
                    height / 2 - halfHeight,
                    width / 2 + halfWidth,
                    height / 2 + halfHeight,
                    yaw,
                    pitch);
        }

        graphics.drawCenteredString(font, title, width / 2, 10, 0x000000);
        graphics.drawCenteredString(
                font,
                Component.translatable("screen.mccreatorutils.model_preview.help"),
                width / 2,
                height - 20,
                0x555555);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 0) {
            yaw += (float) dragX * 0.6f;
            pitch = Mth.clamp(pitch + (float) dragY * 0.6f, -80.0f, 80.0f);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0.0) {
            float factor = scrollY > 0.0 ? 1.1f : 1.0f / 1.1f;
            zoom = Mth.clamp(zoom * factor, MIN_ZOOM, MAX_ZOOM);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(new CreatorToolsScreen(returnTool));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
