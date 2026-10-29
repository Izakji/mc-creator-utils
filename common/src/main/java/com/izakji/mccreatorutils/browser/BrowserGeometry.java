package com.izakji.mccreatorutils.browser;

/**
 * Immutable browser window rectangle in GUI coordinates.
 *
 * @param x left coordinate
 * @param y top coordinate
 * @param width window width
 * @param height window height
 * @author Izakji
 */
public record BrowserGeometry(int x, int y, int width, int height) {

    /**
     * Centers a window of the requested size on the screen.
     *
     * @param screenWidth screen width
     * @param screenHeight screen height
     * @param width desired window width
     * @param height desired window height
     * @return centered geometry
     */
    public static BrowserGeometry centered(int screenWidth, int screenHeight, int width, int height) {
        return new BrowserGeometry((screenWidth - width) / 2, (screenHeight - height) / 2, width, height);
    }

    /**
     * Fills the screen with a uniform margin.
     *
     * @param screenWidth screen width
     * @param screenHeight screen height
     * @param margin margin in GUI pixels
     * @return fullscreen geometry
     */
    public static BrowserGeometry fullscreen(int screenWidth, int screenHeight, int margin) {
        return new BrowserGeometry(
                margin, margin, Math.max(1, screenWidth - margin * 2), Math.max(1, screenHeight - margin * 2));
    }

    /**
     * Clamps this window into the screen while respecting the requested minimum size.
     *
     * @param screenWidth screen width
     * @param screenHeight screen height
     * @param minWidth minimum width
     * @param minHeight minimum height
     * @return clamped geometry
     */
    public BrowserGeometry clampTo(int screenWidth, int screenHeight, int minWidth, int minHeight) {
        int clampedWidth = Math.max(1, Math.min(Math.max(width, minWidth), Math.max(1, screenWidth)));
        int clampedHeight = Math.max(1, Math.min(Math.max(height, minHeight), Math.max(1, screenHeight)));
        int clampedX = Math.max(0, Math.min(x, Math.max(0, screenWidth - clampedWidth)));
        int clampedY = Math.max(0, Math.min(y, Math.max(0, screenHeight - clampedHeight)));
        return new BrowserGeometry(clampedX, clampedY, clampedWidth, clampedHeight);
    }

    /**
     * Returns the right edge.
     *
     * @return x plus width
     */
    public int right() {
        return x + width;
    }

    /**
     * Returns the bottom edge.
     *
     * @return y plus height
     */
    public int bottom() {
        return y + height;
    }
}
