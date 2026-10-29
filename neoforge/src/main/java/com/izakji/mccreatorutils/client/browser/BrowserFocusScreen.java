package com.izakji.mccreatorutils.client.browser;

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
}
