package com.izakji.mccreatorutils.client.browser;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Browser tab operations used by the Rinku-free workspace and UI.
 *
 * <p>Implementations own one embedded browser and must make every operation safe to call from the
 * Minecraft client thread. A closed handle must ignore further input.
 *
 * @author Izakji
 */
public interface BrowserTabHandle {
    /**
     * Returns the current page URL.
     *
     * @return current URL, or an empty string before navigation
     */
    String url();

    /**
     * Navigates this tab to a URL.
     *
     * @param url normalized URL
     */
    void load(String url);

    /**
     * Resizes the browser texture.
     *
     * @param width texture width in pixels
     * @param height texture height in pixels
     */
    void resize(int width, int height);

    /**
     * Draws the current browser texture.
     *
     * @param graphics draw context
     * @param x left screen coordinate
     * @param y top screen coordinate
     * @param width draw width
     * @param height draw height
     */
    void render(GuiGraphics graphics, int x, int y, int width, int height);

    /**
     * Returns whether the first browser frame is available.
     *
     * @return true when the texture can be rendered
     */
    boolean isTextureReady();

    /**
     * Returns whether the page is loading.
     *
     * @return true while a navigation is in progress
     */
    boolean isLoading();

    /**
     * Returns whether history can go back.
     *
     * @return true when a previous page exists
     */
    boolean canGoBack();

    /**
     * Returns whether history can go forward.
     *
     * @return true when a forward page exists
     */
    boolean canGoForward();

    /**
     * Navigates to the previous history entry.
     */
    void goBack();

    /**
     * Navigates to the next history entry.
     */
    void goForward();

    /**
     * Reloads the current page.
     */
    void reload();

    /**
     * Enables or disables browser input focus.
     *
     * @param focused true to route keyboard input to the page
     */
    void setFocus(boolean focused);

    /**
     * Sends a mouse press in browser texture coordinates.
     *
     * @param x horizontal texture coordinate
     * @param y vertical texture coordinate
     * @param button mouse button
     */
    void sendMousePress(int x, int y, int button);

    /**
     * Sends a mouse release in browser texture coordinates.
     *
     * @param x horizontal texture coordinate
     * @param y vertical texture coordinate
     * @param button mouse button
     */
    void sendMouseRelease(int x, int y, int button);

    /**
     * Sends a mouse move in browser texture coordinates.
     *
     * @param x horizontal texture coordinate
     * @param y vertical texture coordinate
     */
    void sendMouseMove(int x, int y);

    /**
     * Sends a mouse wheel event in browser texture coordinates.
     *
     * @param x horizontal texture coordinate
     * @param y vertical texture coordinate
     * @param amount vertical scroll amount
     * @param modifiers input modifiers
     */
    void sendMouseWheel(int x, int y, double amount, int modifiers);

    /**
     * Sends a key press.
     *
     * @param keyCode GLFW key code
     * @param scanCode platform scan code
     * @param modifiers input modifiers
     */
    void sendKeyPress(int keyCode, int scanCode, int modifiers);

    /**
     * Sends a key release.
     *
     * @param keyCode GLFW key code
     * @param scanCode platform scan code
     * @param modifiers input modifiers
     */
    void sendKeyRelease(int keyCode, int scanCode, int modifiers);

    /**
     * Sends a typed character.
     *
     * @param codePoint Unicode code point
     * @param modifiers input modifiers
     */
    void sendKeyTyped(char codePoint, int modifiers);

    /**
     * Closes the embedded browser and releases its native resources.
     *
     * <p>Idempotent.
     */
    void close();
}
