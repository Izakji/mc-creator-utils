package com.izakji.mccreatorutils.client.browser;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Browser tab operations used by the Rinku-free workspace and UI.
 *
 * @author Izakji
 */
public interface BrowserTabHandle {
    String url();

    void load(String url);

    void resize(int width, int height);

    void render(GuiGraphics graphics, int x, int y, int width, int height);

    boolean isTextureReady();

    boolean isLoading();

    boolean canGoBack();

    boolean canGoForward();

    void goBack();

    void goForward();

    void reload();

    void setFocus(boolean focused);

    void sendMousePress(int x, int y, int button);

    void sendMouseRelease(int x, int y, int button);

    void sendMouseMove(int x, int y);

    void sendMouseWheel(int x, int y, double amount, int modifiers);

    void sendKeyPress(int keyCode, int scanCode, int modifiers);

    void sendKeyRelease(int keyCode, int scanCode, int modifiers);

    void sendKeyTyped(char codePoint, int modifiers);

    void close();
}
