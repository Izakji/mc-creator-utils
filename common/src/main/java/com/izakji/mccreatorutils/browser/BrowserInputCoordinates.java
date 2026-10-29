package com.izakji.mccreatorutils.browser;

/**
 * Converts GUI-space mouse positions into Rinku browser texture pixels.
 *
 * @author Izakji
 */
public final class BrowserInputCoordinates {
    private BrowserInputCoordinates() {}

    /**
     * Converts one GUI coordinate to a browser texture pixel.
     *
     * @param guiCoordinate mouse coordinate in GUI space
     * @param contentOrigin content rectangle origin in GUI space
     * @param guiScale Minecraft GUI scale
     * @return browser texture pixel coordinate
     */
    public static int toTexturePixel(double guiCoordinate, int contentOrigin, double guiScale) {
        return (int) ((guiCoordinate - contentOrigin) * guiScale);
    }
}
