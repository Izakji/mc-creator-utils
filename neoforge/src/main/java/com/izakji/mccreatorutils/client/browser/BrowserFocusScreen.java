package com.izakji.mccreatorutils.client.browser;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Transparent focus screen for the browser workspace.
 *
 * @author Izakji
 */
public final class BrowserFocusScreen extends Screen {
    public BrowserFocusScreen() {
        super(Component.translatable("screen.mccreatorutils.browser"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        BrowserWorkspaceRenderer.render(graphics, BrowserWorkspace.instance(), false);
    }
}
